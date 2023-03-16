package io.quarkus.qute.deployment;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

public class RecordCheckedTemplateEnhancer extends CheckedTemplateEnhancer {

    private static final String TEMPLATE_INSTANCE_FIELD = "$wrapped";

    private final CheckedTemplateMethod constructorInfo;

    public RecordCheckedTemplateEnhancer(MethodInfo methodInfo, String templatePath, String fragmentId,
            List<String> parameterNames,
            CheckedTemplateAdapter adaptor) {
        constructorInfo = new CheckedTemplateMethod(methodInfo, templatePath, fragmentId, parameterNames, adaptor);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new DynamicTemplateClassVisitor(className, constructorInfo, outputClassVisitor);
    }

    public static class DynamicTemplateClassVisitor extends ClassVisitor {

        private final CheckedTemplateMethod constructorInfo;
        private String binaryClassName;

        public DynamicTemplateClassVisitor(String className, CheckedTemplateMethod constructorInfo,
                ClassVisitor outputClassVisitor) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.constructorInfo = constructorInfo;
            this.binaryClassName = className.replace('.', '/');
        }

        @Override
        public void visitEnd() {
            super.visitField(Modifier.PRIVATE | Modifier.FINAL, TEMPLATE_INSTANCE_FIELD, "Lio/quarkus/qute/TemplateInstance;",
                    null, null).visitEnd();

            MethodVisitor wrapped = super.visitMethod(Modifier.PUBLIC, "wrapped", "()Lio/quarkus/qute/TemplateInstance;", null,
                    null);
            wrapped.visitIntInsn(Opcodes.ALOAD, 0);
            wrapped.visitFieldInsn(Opcodes.GETFIELD, binaryClassName, TEMPLATE_INSTANCE_FIELD,
                    "Lio/quarkus/qute/TemplateInstance;");
            wrapped.visitInsn(Opcodes.ARETURN);
            wrapped.visitMaxs(0, 0);
            wrapped.visitEnd();

            super.visitEnd();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<init>")) {
                return new TemplateConstructorVisitor(constructorInfo, binaryClassName, ret);
            }
            return ret;
        }

        public static class TemplateConstructorVisitor extends CheckedTemplateMethodVisitor {

            private String binaryClassName;

            public TemplateConstructorVisitor(CheckedTemplateMethod recordConstructor, String binaryClassName,
                    MethodVisitor outputVisitor) {
                super(recordConstructor, outputVisitor);
                this.binaryClassName = binaryClassName;
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.RETURN) {
                    visitIntInsn(Opcodes.ALOAD, 0);
                    setupTemplateInstanceOnStack();
                    /*
                     * this.$wrapped = wrapped;
                     */
                    visitFieldInsn(Opcodes.PUTFIELD, binaryClassName, TEMPLATE_INSTANCE_FIELD,
                            "Lio/quarkus/qute/TemplateInstance;");
                }
                super.visitInsn(opcode);
            }
        }
    }
}
