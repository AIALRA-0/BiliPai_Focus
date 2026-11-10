package com.android.purebilibili.build;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class WorkManagerNamespaceGuard
        implements AsmClassVisitorFactory<InstrumentationParameters.None> {
    private static final String TARGET = "androidx.work.impl.background.systemjob.JobScheduler34";
    private static final String CLEANUP_TARGET = "androidx.work.impl.background.systemjob.SystemJobScheduler";
    private static final String SCHEDULER = "android/app/job/JobScheduler";
    private static final String ACCESSOR = "(L" + SCHEDULER + ";)L" + SCHEDULER + ";";

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return TARGET.equals(classData.getClassName()) || CLEANUP_TARGET.equals(classData.getClassName());
    }

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor next) {
        return guard(next);
    }

    static ClassVisitor guard(ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            private boolean cleanupClass;
            private boolean accessorFound;
            private boolean namespaceCallFound;
            private boolean cleanupFound;

            @Override
            public void visit(int version, int access, String name, String signature,
                    String superName, String[] interfaces) {
                cleanupClass = CLEANUP_TARGET.replace('.', '/').equals(name);
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (cleanupClass && "cancelAllInAllNamespaces".equals(name)
                        && "(Landroid/content/Context;)V".equals(descriptor)
                        && (access & Opcodes.ACC_STATIC) != 0) {
                    return new MethodVisitor(Opcodes.ASM9, method) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                String descriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEVIRTUAL && SCHEDULER.equals(owner)
                                    && "cancelAll".equals(name) && "()V".equals(descriptor)) {
                                cleanupFound = true;
                                Label namespaced = new Label();
                                Label done = new Label();
                                super.visitInsn(Opcodes.DUP);
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitLdcInsn("jobscheduler");
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/content/Context",
                                        "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;", false);
                                super.visitJumpInsn(Opcodes.IF_ACMPNE, namespaced);
                                super.visitInsn(Opcodes.POP);
                                super.visitJumpInsn(Opcodes.GOTO, done);
                                super.visitLabel(namespaced);
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                super.visitLabel(done);
                                return;
                            }
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    };
                }
                if (!"forNamespace".equals(name) || !ACCESSOR.equals(descriptor)) {
                    return method;
                }
                accessorFound = true;
                int schedulerArgument = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
                return new MethodVisitor(Opcodes.ASM9, method) {
                    private final Label tryStart = new Label();
                    private final Label tryEnd = new Label();
                    private final Label missingApi = new Label();

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitTryCatchBlock(tryStart, tryEnd, missingApi, "java/lang/NoSuchMethodError");
                        super.visitTryCatchBlock(tryStart, tryEnd, missingApi, "java/lang/AbstractMethodError");
                        Label supportedSdk = new Label();
                        super.visitFieldInsn(Opcodes.GETSTATIC, "android/os/Build$VERSION", "SDK_INT", "I");
                        super.visitIntInsn(Opcodes.BIPUSH, 34);
                        super.visitJumpInsn(Opcodes.IF_ICMPGE, supportedSdk);
                        super.visitVarInsn(Opcodes.ALOAD, schedulerArgument);
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitLabel(supportedSdk);
                        super.visitLabel(tryStart);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                            String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && SCHEDULER.equals(owner)
                                && "forNamespace".equals(name)
                                && ("(Ljava/lang/String;)L" + SCHEDULER + ";").equals(descriptor)) {
                            namespaceCallFound = true;
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitLabel(tryEnd);
                        super.visitLabel(missingApi);
                        super.visitInsn(Opcodes.POP);
                        super.visitVarInsn(Opcodes.ALOAD, schedulerArgument);
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitMaxs(Math.max(maxStack, 2), maxLocals);
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (cleanupClass ? !cleanupFound : !accessorFound || !namespaceCallFound) {
                    throw new IllegalStateException("WorkManager internals changed: review namespace compatibility guard");
                }
                super.visitEnd();
            }
        };
    }
}
