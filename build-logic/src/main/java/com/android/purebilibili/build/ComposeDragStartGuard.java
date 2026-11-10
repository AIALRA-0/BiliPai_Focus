package com.android.purebilibili.build;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compose Foundation compatibility patch: DragGestureNode checks canDrag only in sendDragStart,
 * after consuming the pointer and before unconditionally sending DragDelta. A rejected pointer
 * (notably a mouse in ScrollableNode) therefore reaches a channel that was never initialized.
 * Reject it while still in AwaitDown, before consumption or any drag-state transition.
 * Matches the cached Foundation 1.13.0-alpha01 class signatures. This is not a catch-all
 * exception handler: accepted touch/stylus input follows the original bytecode unchanged.
 * Remove when upstream performs the eligibility check before leaving AwaitDown.
 */
public abstract class ComposeDragStartGuard
        implements AsmClassVisitorFactory<InstrumentationParameters.None> {
    private static final String TARGET = "androidx.compose.foundation.gestures.DragGestureNode";
    private static final String OWNER = TARGET.replace('.', '/');
    private static final String EVENT = "androidx/compose/ui/input/pointer/PointerEvent";
    private static final String CHANGE = "androidx/compose/ui/input/pointer/PointerInputChange";
    private static final String TYPE = "androidx/compose/ui/input/pointer/PointerType";
    private static final String PREDICATE = "kotlin/jvm/functions/Function1";
    private static final String INITIAL_DOWN = "(L" + EVENT
            + ";Landroidx/compose/ui/input/pointer/PointerEventPass;"
            + "Landroidx/compose/foundation/gestures/DragDetectionState$AwaitDown;)V";

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return TARGET.equals(classData.getClassName());
    }

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            private boolean predicateFound;
            private boolean initialDownFound;

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                    String signature, Object value) {
                if ("canDrag".equals(name) && ("L" + PREDICATE + ";").equals(descriptor)) {
                    predicateFound = true;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"processInitialDownState".equals(name) || !INITIAL_DOWN.equals(descriptor)) {
                    return method;
                }
                if ((access & Opcodes.ACC_STATIC) != 0) {
                    throw new IllegalStateException("Compose drag entry changed: review pointer eligibility patch");
                }
                initialDownFound = true;
                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label nonEmpty = new Label();
                        Label accepted = new Label();
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EVENT, "getChanges", "()Ljava/util/List;", false);
                        super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);
                        super.visitJumpInsn(Opcodes.IFEQ, nonEmpty);
                        super.visitInsn(Opcodes.RETURN);
                        super.visitLabel(nonEmpty);

                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, OWNER, "canDrag", "L" + PREDICATE + ";");
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EVENT, "getChanges", "()Ljava/util/List;", false);
                        super.visitInsn(Opcodes.ICONST_0);
                        super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
                        super.visitTypeInsn(Opcodes.CHECKCAST, CHANGE);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CHANGE, "getType-T8wyACA", "()I", false);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE, "box-impl", "(I)L" + TYPE + ";", false);
                        super.visitMethodInsn(Opcodes.INVOKEINTERFACE, PREDICATE, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                        super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                        super.visitJumpInsn(Opcodes.IFNE, accepted);
                        super.visitInsn(Opcodes.RETURN);
                        super.visitLabel(accepted);
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (!predicateFound || !initialDownFound) {
                    throw new IllegalStateException("Compose internals changed: review drag pointer eligibility patch");
                }
                super.visitEnd();
            }
        };
    }
}
