package io.quarkus.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassOperationGenerationVisitor;

public class PanacheJpaRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public PanacheJpaRepositoryEnhancer(IndexView index, TypeBundle bundle) {
        super(index, bundle);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new Hibernate63Visitor(className, outputClassVisitor,
                this.indexView, this.bundle);
    }

    public static class Hibernate63Visitor extends PanacheRepositoryClassOperationGenerationVisitor {

        public Hibernate63Visitor(String className, ClassVisitor outputClassVisitor, IndexView indexView,
                TypeBundle typeBundle) {
            super(className, outputClassVisitor, indexView, typeBundle);
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            MethodInfo methodIfNative = null;
            // special case for instance native methods with @Find/@HQL/@SQL annotations
            if (!Modifier.isStatic(access)
                    && Modifier.isPublic(access)
                    && Modifier.isNative(access)
                    && (access & Opcodes.ACC_SYNTHETIC) == 0) {
                org.jboss.jandex.Type[] argTypes = AsmUtil.getParameterTypes(descriptor);
                MethodInfo method = this.daoClassInfo.method(methodName, argTypes);
                if (method == null) {
                    throw new IllegalStateException(
                            "Could not find indexed method: " + daoClassInfo + "." + methodName + " with descriptor "
                                    + descriptor
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
                        daoClassInfo.name().toString('/'), typeBundle, false);
            }
            return superVisitor;
        }

    }
}
