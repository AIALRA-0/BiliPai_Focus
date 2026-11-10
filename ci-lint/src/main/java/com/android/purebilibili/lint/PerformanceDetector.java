package com.android.purebilibili.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.SourceCodeScanner;
import com.android.tools.lint.detector.api.SourceSetType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import java.util.Objects;
import org.jetbrains.uast.UBinaryExpression;
import org.jetbrains.uast.UBlockExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UIfExpression;
import org.jetbrains.uast.ULambdaExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UNamedExpression;
import org.jetbrains.uast.UParenthesizedExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.USwitchClauseExpressionWithBody;
import org.jetbrains.uast.USwitchExpression;
import org.jetbrains.uast.UYieldExpression;
import org.jetbrains.uast.UastBinaryOperator;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

/** Checks allocations in the resolved Compose layer-update callback, not frame timings. */
public final class PerformanceDetector extends Detector implements SourceCodeScanner {
    public static final Issue ISSUE = Issue.create(
            "BiliPaiFrameBlurAllocation",
            "Blur effect creation in a layer update callback",
            "Android RenderEffect.createBlurEffect creates an effect object. Inside Compose's "
                    + "graphicsLayer update callback this call can execute again on layer updates. "
                    + "Prebuild effects outside the callback or retain an effect with a radius-keyed "
                    + "cache. This check identifies a static allocation site, not a measured frame "
                    + "cost; unrecognized cache/control-flow patterns can be reviewed and suppressed "
                    + "with the standard Android Lint suppression or baseline mechanisms.",
            Category.PERFORMANCE,
            6,
            Severity.ERROR,
            new Implementation(PerformanceDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public List<String> getApplicableMethodNames() {
        return List.of("createBlurEffect");
    }

    @Override
    public void visitMethodCall(JavaContext context, UCallExpression node, PsiMethod method) {
        if (context.getSourceSetType() != SourceSetType.MAIN
                || !context.getEvaluator().isMemberInClass(method, "android.graphics.RenderEffect")) {
            return;
        }
        ULambdaExpression layer = layerCallback(context, node);
        if (layer == null || hasRadiusCache(context, node, layer)) {
            return;
        }
        context.report(ISSUE, node, context.getNameLocation(node),
                "createBlurEffect is called inside a graphicsLayer update callback; "
                        + "a retained radius-keyed effect cache could not be established. "
                        + "This allocation can repeat on layer updates.");
    }

    private static ULambdaExpression layerCallback(JavaContext context, UCallExpression node) {
        for (UElement parent = node.getUastParent(); parent != null;
                parent = parent.getUastParent()) {
            // A nested declared function is not executed merely by declaring it in the callback.
            if (parent instanceof UMethod) {
                return null;
            }
            if (!(parent instanceof ULambdaExpression lambda)) {
                continue;
            }
            UElement owner = lambda.getUastParent();
            while (owner instanceof UNamedExpression || owner instanceof UParenthesizedExpression) {
                owner = owner.getUastParent();
            }
            if (!(owner instanceof UCallExpression call)) {
                continue;
            }
            PsiMethod resolved = call.resolve();
            if (resolved == null
                    || !"graphicsLayer".equals(resolved.getName())
                    || !context.getEvaluator().isMemberInClass(resolved,
                            "androidx.compose.ui.graphics.GraphicsLayerModifierKt")
                    || resolved.getParameterList().getParametersCount() == 0
                    || !context.getEvaluator().parameterHasType(resolved, 0,
                            "androidx.compose.ui.Modifier")) {
                continue;
            }
            // The float/boolean graphicsLayer overload is not a layer-update callback.
            int last = resolved.getParameterList().getParametersCount() - 1;
            UExpression argument = call.getArgumentForParameter(last);
            if (argument != null && Objects.equals(unwrap(argument), lambda)) {
                return lambda;
            }
        }
        return null;
    }

    private static boolean hasRadiusCache(
            JavaContext context, UCallExpression allocation, ULambdaExpression layer) {
        UExpression x = allocation.getArgumentForParameter(0);
        UExpression y = allocation.getArgumentForParameter(1);
        if (x == null || y == null) {
            return false;
        }
        for (UElement parent = allocation.getUastParent(); parent != null && parent != layer;
                parent = parent.getUastParent()) {
            if (parent instanceof UIfExpression guard
                    && isWithin(allocation, guard.getThenExpression())) {
                UExpression branch = guard.getThenExpression();
                List<UExpression> statements = branch instanceof UBlockExpression block
                        ? block.getExpressions() : List.of(branch);
                if (storesEffect(context, statements, allocation, layer)
                        && guardsRadius(guard.getCondition(), x, statements, layer)
                        && guardsRadius(guard.getCondition(), y, statements, layer)) {
                    return true;
                }
            } else if (parent instanceof USwitchClauseExpressionWithBody branch
                    && isWithin(allocation, branch.getBody())) {
                UElement owner = branch.getUastParent();
                while (owner != null && !(owner instanceof USwitchExpression)) {
                    owner = owner.getUastParent();
                }
                // Only subjectless when entries contain boolean cache guards. A subject's
                // case labels and an else entry do not establish radius inequality.
                if (!(owner instanceof USwitchExpression selection)
                        || selection.getExpression() != null || branch.getCaseValues().isEmpty()) {
                    continue;
                }
                List<UExpression> statements = branch.getBody().getExpressions();
                if (!storesEffect(context, statements, allocation, layer)) {
                    continue;
                }
                boolean guarded = true;
                // Comma-separated when conditions are alternatives: every alternative must
                // guard both allocation radii, not just an unrelated condition in the list.
                for (UExpression condition : branch.getCaseValues()) {
                    if (!guardsRadius(condition, x, statements, layer)
                            || !guardsRadius(condition, y, statements, layer)) {
                        guarded = false;
                        break;
                    }
                }
                if (guarded) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean guardsRadius(UExpression condition, UExpression radius,
            List<UExpression> statements, ULambdaExpression layer) {
        condition = unwrap(condition);
        if (!(condition instanceof UBinaryExpression comparison)) {
            return false;
        }
        if (comparison.getOperator() == UastBinaryOperator.LOGICAL_AND) {
            return guardsRadius(comparison.getLeftOperand(), radius, statements, layer)
                    || guardsRadius(comparison.getRightOperand(), radius, statements, layer);
        }
        if (comparison.getOperator() != UastBinaryOperator.NOT_EQUALS) {
            return false;
        }
        UExpression key;
        if (sameValue(comparison.getLeftOperand(), radius)) {
            key = comparison.getRightOperand();
        } else if (sameValue(comparison.getRightOperand(), radius)) {
            key = comparison.getLeftOperand();
        } else {
            return false;
        }
        if (!retainedReference(key, layer)) {
            return false;
        }
        for (UExpression statement : statements) {
            if (unwrap(statement) instanceof UBinaryExpression write
                    && write.getOperator() == UastBinaryOperator.ASSIGN
                    && sameValue(write.getLeftOperand(), key)
                    && sameValue(write.getRightOperand(), radius)
                    && writeCount(layer, key) == 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean storesEffect(JavaContext context, List<UExpression> statements,
            UCallExpression allocation, ULambdaExpression layer) {
        for (UExpression statement : statements) {
            if (unwrap(statement) instanceof UBinaryExpression write
                    && write.getOperator() == UastBinaryOperator.ASSIGN
                    && retainedReference(write.getLeftOperand(), layer)
                    && storedAllocation(context, write.getRightOperand(), allocation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean storedAllocation(
            JavaContext context, UExpression expression, UCallExpression allocation) {
        expression = unwrap(expression);
        if (Objects.equals(expression, allocation)) {
            return true;
        }
        if (expression instanceof UQualifiedReferenceExpression qualified) {
            return storedAllocation(context, qualified.getSelector(), allocation);
        }
        if (expression instanceof UCallExpression conversion) {
            PsiMethod method = conversion.resolve();
            if (method != null && "asComposeRenderEffect".equals(method.getName())
                    && context.getEvaluator().getPackage(method) != null
                    && "androidx.compose.ui.graphics".equals(
                            context.getEvaluator().getPackage(method).getQualifiedName())
                    && context.getEvaluator().parameterHasType(method, 0,
                            "android.graphics.RenderEffect")) {
                UExpression receiver = conversion.getReceiver();
                if (receiver == null) {
                    receiver = conversion.getArgumentForParameter(0);
                }
                return receiver != null && storedAllocation(context, receiver, allocation);
            }
        }
        return false;
    }

    private static int writeCount(ULambdaExpression layer, UExpression key) {
        int[] count = {0};
        layer.getBody().accept(new AbstractUastVisitor() {
            @Override
            public boolean visitBinaryExpression(UBinaryExpression expression) {
                if (expression.getOperator() instanceof UastBinaryOperator.AssignOperator
                        && sameValue(expression.getLeftOperand(), key)) {
                    count[0]++;
                }
                return false;
            }
        });
        return count[0];
    }

    private static boolean retainedReference(UExpression expression, ULambdaExpression layer) {
        expression = unwrap(expression);
        if (!(expression instanceof UReferenceExpression reference)) {
            return false;
        }
        PsiElement symbol = reference.resolve();
        if (symbol == null || layer.getSourcePsi() == null
                || PsiTreeUtil.isAncestor(layer.getSourcePsi(), symbol.getNavigationElement(), false)) {
            return false;
        }
        // A field on an object newly declared in the callback is not a retained cache either.
        return !(expression instanceof UQualifiedReferenceExpression qualified)
                || retainedReference(qualified.getReceiver(), layer);
    }

    private static boolean sameValue(UExpression first, UExpression second) {
        first = unwrap(first);
        second = unwrap(second);
        if (first instanceof UQualifiedReferenceExpression a
                && second instanceof UQualifiedReferenceExpression b) {
            return sameValue(a.getReceiver(), b.getReceiver())
                    && sameValue(a.getSelector(), b.getSelector());
        }
        if (first instanceof UQualifiedReferenceExpression
                || second instanceof UQualifiedReferenceExpression) {
            return false;
        }
        if (first instanceof UReferenceExpression a && second instanceof UReferenceExpression b) {
            PsiElement resolvedA = a.resolve();
            PsiElement resolvedB = b.resolve();
            return resolvedA != null && resolvedB != null
                    && Objects.equals(resolvedA.getNavigationElement(), resolvedB.getNavigationElement());
        }
        return first instanceof ULiteralExpression a && second instanceof ULiteralExpression b
                && a.getValue() != null && a.getValue().equals(b.getValue());
    }

    private static UExpression unwrap(UExpression expression) {
        while (true) {
            if (expression instanceof UParenthesizedExpression parentheses) {
                expression = parentheses.getExpression();
            } else if (expression instanceof UNamedExpression named) {
                expression = named.getExpression();
            } else if (expression instanceof UYieldExpression yielded) {
                // Kotlin when wraps the final branch statement in a synthetic yield.
                expression = yielded.getExpression();
            } else {
                return expression;
            }
        }
    }

    private static boolean isWithin(UElement element, UElement ancestor) {
        for (UElement current = element; current != null; current = current.getUastParent()) {
            if (Objects.equals(current, ancestor)) {
                return true;
            }
        }
        return false;
    }
}
