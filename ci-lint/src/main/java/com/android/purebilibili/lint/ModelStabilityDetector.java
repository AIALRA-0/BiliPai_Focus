package com.android.purebilibili.lint;

import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.SourceCodeScanner;
import com.android.tools.lint.detector.api.SourceSetType;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jetbrains.kotlin.analysis.api.AnalyzeKt;
import org.jetbrains.kotlin.analysis.api.KaSession;
import org.jetbrains.kotlin.analysis.api.types.KaClassType;
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType;
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType;
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType;
import org.jetbrains.kotlin.analysis.api.types.KaType;
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UField;

/** Enforces the declaration policy behind the model package's Compose stability promise. */
public final class ModelStabilityDetector extends Detector implements SourceCodeScanner {
    private static final String MODEL_PACKAGE = "com.android.purebilibili.data.model";

    public static final Issue ISSUE = Issue.create(
            "BiliPaiModelStability",
            "Mutable state in a package declared Compose-stable",
            "compose_stability.conf declares com.android.purebilibili.data.model and its "
                    + "subpackages stable. Model properties must not be var/non-final fields or "
                    + "have mutable collection or array types, including private caches. Kotlin "
                    + "read-only collection interfaces are allowed by this declaration policy; "
                    + "this check is not a proof of deep immutability or of runtime mutation.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(ModelStabilityDetector.class, Scope.JAVA_FILE_SCOPE));

    private static final Set<String> MUTABLE_KOTLIN_TYPES = Set.of(
            "kotlin.collections.MutableCollection", "kotlin.collections.MutableList",
            "kotlin.collections.MutableMap", "kotlin.collections.MutableMap.MutableEntry",
            "kotlin.collections.MutableSet", "kotlin.collections.MutableIterator",
            "kotlin.collections.MutableListIterator",
            "kotlin.Array", "kotlin.BooleanArray", "kotlin.ByteArray", "kotlin.CharArray",
            "kotlin.ShortArray", "kotlin.IntArray", "kotlin.LongArray", "kotlin.FloatArray",
            "kotlin.DoubleArray", "kotlin.UByteArray", "kotlin.UShortArray",
            "kotlin.UIntArray", "kotlin.ULongArray");

    private static final Set<String> MUTABLE_JAVA_TYPES = Set.of(
            "java.util.Collection", "java.util.List", "java.util.Set", "java.util.Map",
            "java.util.Map.Entry", "java.util.Iterator", "java.util.ListIterator",
            "java.util.Queue", "java.util.Deque", "java.util.ArrayList",
            "java.util.LinkedList", "java.util.HashMap", "java.util.LinkedHashMap",
            "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeMap",
            "java.util.TreeSet", "java.util.EnumMap", "java.util.EnumSet",
            "java.util.IdentityHashMap", "java.util.WeakHashMap", "java.util.Vector",
            "java.util.Stack", "java.util.Hashtable", "java.util.PriorityQueue",
            "java.util.ArrayDeque", "java.util.concurrent.ConcurrentHashMap",
            "java.util.concurrent.CopyOnWriteArrayList", "java.util.concurrent.CopyOnWriteArraySet");

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return List.of(UClass.class);
    }

    @Override
    public UElementHandler createUastHandler(JavaContext context) {
        if (context.isTestSource() || context.getSourceSetType() != SourceSetType.MAIN) {
            return null;
        }
        return new UElementHandler() {
            @Override
            public void visitClass(UClass node) {
                String packageName = context.getUastFile().getPackageName();
                if (!packageName.equals(MODEL_PACKAGE)
                        && !packageName.startsWith(MODEL_PACKAGE + ".")) {
                    return;
                }
                PsiElement source = node.getSourcePsi();
                if (source instanceof KtClassOrObject model) {
                    checkKotlinClass(context, model);
                } else if (source instanceof PsiClass
                        && !(source.getContainingFile() instanceof KtFile)) {
                    for (UField field : node.getFields()) {
                        boolean mutable = !((PsiField) field.getJavaPsi()).hasModifierProperty(PsiModifier.FINAL);
                        boolean mutableType = hasMutableJavaType(context, field.getType());
                        if (mutable || mutableType) {
                            context.report(ISSUE, field, context.getNameLocation(field),
                                    message(mutable, mutableType));
                        }
                    }
                }
            }
        };
    }

    private static void checkKotlinClass(JavaContext context, KtClassOrObject model) {
        // Enumerating declarations, not descendant variables, excludes getter/function/init locals.
        for (KtParameter parameter : model.getPrimaryConstructorParameters()) {
            if (parameter.hasValOrVar()) {
                checkKotlinProperty(context, parameter, parameter.isMutable());
            }
        }
        for (KtDeclaration declaration : model.getDeclarations()) {
            if (declaration instanceof KtProperty property) {
                checkKotlinProperty(context, property, property.isVar());
            }
        }
    }

    private static void checkKotlinProperty(
            JavaContext context, KtDeclaration declaration, boolean mutable) {
        // PsiType maps both Kotlin List and MutableList to java.util.List. Resolve the Kotlin
        // declaration's semantic type instead, including inferred types and expanded type aliases.
        // A constructor vararg property exposes an array, not its parameter element type.
        boolean mutableType = declaration instanceof KtParameter parameter && parameter.isVarArg()
                || AnalyzeKt.analyze(declaration,
                        session -> hasMutableKotlinType(session, session.getReturnType(declaration)));
        if (mutable || mutableType) {
            context.report(ISSUE, declaration, context.getNameLocation(declaration),
                    message(mutable, mutableType));
        }
    }

    private static String message(boolean mutable, boolean mutableType) {
        String fact = mutable && mutableType ? "a mutable property with a mutable collection/array type"
                : mutable ? "a mutable property" : "a mutable collection/array property type";
        return "This Compose-stable model declares " + fact
                + "; the stability policy also applies to private state. Use immutable properties "
                + "and read-only collection types, or move mutable state outside the stable model package.";
    }

    private static boolean hasMutableKotlinType(KaSession session, KaType type) {
        if (type == null) {
            return false;
        }
        if (type instanceof KaFlexibleType flexible) {
            return hasMutableKotlinType(session, flexible.getLowerBound())
                    || hasMutableKotlinType(session, flexible.getUpperBound());
        }
        if (type instanceof KaDefinitelyNotNullType notNull) {
            return hasMutableKotlinType(session, notNull.getOriginal());
        }
        if (type instanceof KaIntersectionType intersection) {
            for (KaType conjunct : intersection.getConjuncts()) {
                if (hasMutableKotlinType(session, conjunct)) {
                    return true;
                }
            }
        }
        if (isMutableClassType(type)) {
            return true;
        }
        if (type instanceof KaClassType classType) {
            for (KaTypeProjection argument : classType.getTypeArguments()) {
                if (hasMutableKotlinType(session, argument.getType())) {
                    return true;
                }
            }
        }
        // Check resolved inheritance as well (e.g. a differently named ArrayList subclass).
        // Do not recurse into supertype arguments: recursive generic bounds can form cycles.
        Iterator<KaType> supertypes = session.getAllSupertypes(type).iterator();
        while (supertypes.hasNext()) {
            if (isMutableClassType(supertypes.next())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMutableClassType(KaType type) {
        if (!(type instanceof KaClassType classType)) {
            return false;
        }
        String name = classType.getClassId().asSingleFqName().asString();
        return MUTABLE_KOTLIN_TYPES.contains(name) || MUTABLE_JAVA_TYPES.contains(name);
    }

    private static boolean hasMutableJavaType(JavaContext context, PsiType type) {
        if (type instanceof PsiArrayType) {
            return true;
        }
        if (type instanceof PsiWildcardType wildcard) {
            PsiType bound = wildcard.getBound();
            return bound != null && hasMutableJavaType(context, bound);
        }
        if (!(type instanceof PsiClassType classType)) {
            return false;
        }
        PsiClass resolved = classType.resolve();
        if (resolved != null) {
            String name = resolved.getQualifiedName();
            if ((name != null && MUTABLE_JAVA_TYPES.contains(name))
                    || context.getEvaluator().inheritsFrom(resolved, "java.util.Collection", false)
                    || context.getEvaluator().inheritsFrom(resolved, "java.util.Map", false)
                    || context.getEvaluator().inheritsFrom(resolved, "java.util.Iterator", false)) {
                return true;
            }
        }
        for (PsiType argument : classType.getParameters()) {
            if (hasMutableJavaType(context, argument)) {
                return true;
            }
        }
        return false;
    }
}
