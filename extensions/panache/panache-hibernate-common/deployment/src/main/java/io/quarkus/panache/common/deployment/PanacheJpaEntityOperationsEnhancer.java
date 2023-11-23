package io.quarkus.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassOperationGenerationVisitor;

public class PanacheJpaEntityOperationsEnhancer extends PanacheEntityEnhancer {
    private final TypeBundle typeBundle;

    public PanacheJpaEntityOperationsEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle) {
        super(index, methodCustomizers);
        this.typeBundle = typeBundle;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        ClassInfo entityInfo = indexView.getClassByName(DotName.createSimple(className));
        return new Hibernate63Visitor(outputClassVisitor, typeBundle,
                entityInfo, methodCustomizers, indexView);
    }

    public static class Hibernate63Visitor extends PanacheEntityClassOperationGenerationVisitor {

        public Hibernate63Visitor(ClassVisitor outputClassVisitor, TypeBundle typeBundle, ClassInfo entityInfo,
                List<PanacheMethodCustomizer> methodCustomizers, IndexView indexView) {
            super(outputClassVisitor, typeBundle, entityInfo, methodCustomizers, indexView);
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            MethodInfo methodIfNative = null;
            // special case for static native methods with @Find/@HQL/@SQL annotations
            if (Modifier.isStatic(access)
                    && Modifier.isPublic(access)
                    && Modifier.isNative(access)
                    && (access & Opcodes.ACC_SYNTHETIC) == 0) {
                org.jboss.jandex.Type[] argTypes = AsmUtil.getParameterTypes(descriptor);
                MethodInfo method = this.entityInfo.method(methodName, argTypes);
                if (method == null) {
                    throw new IllegalStateException(
                            "Could not find indexed method: " + entityInfo + "." + methodName + " with descriptor " + descriptor
                                    + " and arg types " + Arrays.toString(argTypes));
                }
                if (PanacheHibernateCommonResourceProcessor.isHibernate63Method(method)) {
                    methodIfNative = method;
                    access &= ~Modifier.NATIVE;
                }
            }
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            if (methodIfNative != null) {
                return new PanacheHibernate63DelegatingMethodVisitor(superVisitor, methodIfNative, descriptor,
                        entityInfo.name().toString('/'), typeBundle, true);
            }
            return superVisitor;
        }

    }

}
