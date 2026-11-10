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
 * Local Compose UI compatibility patch: canceled prefetch can dispose slots after their root
 * detached. The unguarded owner lookup then throws instead of taking synchronous deactivation.
 * Preserve the library's executor for attached roots and return null only for detached roots.
 * Remove together with the homepage opt-in once the upstream accessor guards attachment.
 */
public abstract class ComposeDetachedOwnerGuard
        implements AsmClassVisitorFactory<InstrumentationParameters.None> {
    private static final String TARGET = "androidx.compose.ui.layout.LayoutNodeSubcompositionsState";
    private static final String OWNER = TARGET.replace('.', '/');
    private static final String NODE = "androidx/compose/ui/node/LayoutNode";
    private static final String EXECUTOR = "()Landroidx/compose/ui/node/OutOfFrameExecutor;";

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return TARGET.equals(classData.getClassName());
    }

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            private boolean rootFound;
            private boolean accessorFound;
            private boolean ownerLookupFound;

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                    String signature, Object value) {
                if ("root".equals(name) && ("L" + NODE + ";").equals(descriptor)) {
                    rootFound = true;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"getOutOfFrameExecutor".equals(name) || !EXECUTOR.equals(descriptor)) {
                    return method;
                }
                if ((access & Opcodes.ACC_STATIC) != 0) {
                    throw new IllegalStateException("Compose owner accessor changed: review detached-owner patch");
                }
                accessorFound = true;
                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                            String descriptor, boolean isInterface) {
                        if ("androidx/compose/ui/node/LayoutNodeKt".equals(owner)
                                && "requireOwner".equals(name)
                                && "(Landroidx/compose/ui/node/LayoutNode;)Landroidx/compose/ui/node/Owner;".equals(descriptor)) {
                            ownerLookupFound = true;
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label attached = new Label();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, OWNER, "root", "L" + NODE + ";");
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NODE, "isAttached", "()Z", false);
                        super.visitJumpInsn(Opcodes.IFNE, attached);
                        super.visitInsn(Opcodes.ACONST_NULL);
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitLabel(attached);
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (!rootFound || !accessorFound || !ownerLookupFound) {
                    throw new IllegalStateException("Compose internals changed: review detached-owner patch before enabling prefetch");
                }
                super.visitEnd();
            }
        };
    }
}
