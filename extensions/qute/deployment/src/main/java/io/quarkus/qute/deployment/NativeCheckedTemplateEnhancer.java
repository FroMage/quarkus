package io.quarkus.qute.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

public class NativeCheckedTemplateEnhancer extends CheckedTemplateEnhancer {

    private final Map<String, CheckedTemplateMethod> methods = new HashMap<>();

    public void implement(MethodInfo methodInfo, String templatePath, String fragmentId, List<String> parameterNames,
            CheckedTemplateAdapter adaptor) {
        // FIXME: this should support overloading by using the method signature as key, but requires moving JandexUtil stuff around
        methods.put(methodInfo.name(),
                new CheckedTemplateMethod(methodInfo, templatePath, fragmentId, parameterNames, adaptor));
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new DynamicTemplateClassVisitor(className, methods, outputClassVisitor);
    }

    public static class DynamicTemplateClassVisitor extends ClassVisitor {

        private final Map<String, CheckedTemplateMethod> methods;

        public DynamicTemplateClassVisitor(String className, Map<String, CheckedTemplateMethod> methods,
                ClassVisitor outputClassVisitor) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.methods = methods;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            CheckedTemplateMethod nativeMethod = methods.get(name);
            if (nativeMethod != null) {
                // remove the native bit
                access = access & ~Modifier.NATIVE;
            }
            MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (nativeMethod != null) {
                return new NativeMethodVisitor(nativeMethod, ret);
            }
            return ret;
        }

        public static class NativeMethodVisitor extends CheckedTemplateMethodVisitor {

            public NativeMethodVisitor(CheckedTemplateMethod nativeMethod, MethodVisitor outputVisitor) {
                super(nativeMethod, outputVisitor);
            }

            @Override
            public void visitEnd() {
                visitCode();
                setupTemplateInstanceOnStack();
                /*
                 * return instance;
                 */
                visitInsn(Opcodes.ARETURN);

                visitMaxs(0, 0);
                super.visitEnd();
            }
        }
    }
}
