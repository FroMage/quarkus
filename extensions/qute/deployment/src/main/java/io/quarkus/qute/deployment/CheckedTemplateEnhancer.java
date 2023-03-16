package io.quarkus.qute.deployment;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.qute.runtime.TemplateProducer;

public abstract class CheckedTemplateEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    protected static class CheckedTemplateMethod {
        private final MethodInfo methodInfo;
        private final String templateId;
        private final String fragmentId;
        private final List<String> parameterNames;
        private final CheckedTemplateAdapter adaptor;

        public CheckedTemplateMethod(MethodInfo methodInfo, String templatePath, String fragmentId, List<String> parameterNames,
                CheckedTemplateAdapter adaptor) {
            this.methodInfo = methodInfo;
            this.templateId = templatePath;
            this.fragmentId = fragmentId;
            this.parameterNames = parameterNames;
            this.adaptor = adaptor;
        }
    }

    public static abstract class CheckedTemplateMethodVisitor extends MethodVisitor {

        private CheckedTemplateMethod nativeMethod;

        public CheckedTemplateMethodVisitor(CheckedTemplateMethod nativeMethod, MethodVisitor outputVisitor) {
            super(Gizmo.ASM_API_VERSION, outputVisitor);
            this.nativeMethod = nativeMethod;
        }

        protected void setupTemplateInstanceOnStack() {
            /*
             * Template template =
             * Arc.container().instance(TemplateProducer.class).get().getInjectableTemplate("HelloResource/typedTemplate");
             */
            visitMethodInsn(Opcodes.INVOKESTATIC, "io/quarkus/arc/Arc", "container", "()Lio/quarkus/arc/ArcContainer;",
                    false);
            visitLdcInsn(org.objectweb.asm.Type.getType(TemplateProducer.class));
            visitLdcInsn(0);
            visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/annotation/Annotation");
            visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/arc/ArcContainer", "instance",
                    "(Ljava/lang/Class;[Ljava/lang/annotation/Annotation;)Lio/quarkus/arc/InstanceHandle;", true);
            visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/arc/InstanceHandle", "get",
                    "()Ljava/lang/Object;", true);
            visitTypeInsn(Opcodes.CHECKCAST, "io/quarkus/qute/runtime/TemplateProducer");
            visitLdcInsn(nativeMethod.templateId);
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "io/quarkus/qute/runtime/TemplateProducer", "getInjectableTemplate",
                    "(Ljava/lang/String;)Lio/quarkus/qute/Template;", false);

            if (nativeMethod.fragmentId != null) {
                /*
                 * template = template.getFragment(id);
                 */
                visitLdcInsn(nativeMethod.fragmentId);
                visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/qute/Template", "getFragment",
                        "(Ljava/lang/String;)Lio/quarkus/qute/Template$Fragment;", true);
            }
            /*
             * TemplateInstance instance = template.instance();
             */
            // we store it on the stack because local vars are too much trouble
            visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/qute/Template", "instance",
                    "()Lio/quarkus/qute/TemplateInstance;", true);

            String templateInstanceBinaryName = "io/quarkus/qute/TemplateInstance";
            // some adaptors are required to return a different type such as MailTemplateInstance
            if (nativeMethod.adaptor != null) {
                nativeMethod.adaptor.convertTemplateInstance(this);
                templateInstanceBinaryName = nativeMethod.adaptor.templateInstanceBinaryName();
            }

            // arg slots start at 0 for static methods, 1 for constructors/instance methods
            int slot = nativeMethod.methodInfo.isConstructor() || !Modifier.isStatic(nativeMethod.methodInfo.flags())
                    ? 1
                    : 0;
            List<Type> parameters = nativeMethod.methodInfo.parameterTypes();
            for (int i = 0; i < nativeMethod.parameterNames.size(); i++) {
                Type parameterType = parameters.get(i);
                /*
                 * instance = instance.data("name", name);
                 */
                visitLdcInsn(nativeMethod.parameterNames.get(i)); // first arg name
                visitVarInsn(AsmUtil.getLoadOpcode(parameterType), slot); // slot-th arg value
                AsmUtil.boxIfRequired(this, parameterType);

                visitMethodInsn(Opcodes.INVOKEINTERFACE, templateInstanceBinaryName, "data",
                        "(Ljava/lang/String;Ljava/lang/Object;)L" + templateInstanceBinaryName + ";", true);

                slot += AsmUtil.getParameterSize(parameterType);
            }
        }
    }
}
