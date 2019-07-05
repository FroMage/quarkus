package io.quarkus.panache.rx.deployment;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PanacheRxFieldAccessEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {


    private ModelInfo modelInfo;

    public PanacheRxFieldAccessEnhancer(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new FieldAccessClassVisitor(className, outputClassVisitor, modelInfo);
    }

    static class FieldAccessClassVisitor extends ClassVisitor {

        private String classBinaryName;
        private ModelInfo modelInfo;

        public FieldAccessClassVisitor(String className, ClassVisitor outputClassVisitor, ModelInfo modelInfo) {
            super(Opcodes.ASM6, outputClassVisitor);
            this.modelInfo = modelInfo;
            this.classBinaryName = className.replace('.', '/');
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheRxFieldAccessMethodVisitor(superVisitor, classBinaryName, methodName, descriptor, modelInfo);
        }
    }
}
