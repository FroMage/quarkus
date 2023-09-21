package io.quarkus.panache.common.deployment;

import jakarta.persistence.EntityManager;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.Gizmo;

public class PanacheHibernate63DelegatingMethodVisitor extends MethodVisitor {

    private final static String ENTITY_MANAGER_SIGNATURE = "L" + EntityManager.class.getName().replace('.', '/') + ";";

    private MethodInfo method;
    private String descriptor;
    private String enclosingClassDescriptor;
    private boolean isEntity;

    public PanacheHibernate63DelegatingMethodVisitor(MethodVisitor superVisitor, MethodInfo method, String descriptor,
            String enclosingClassDescriptor, boolean isEntity) {
        super(Gizmo.ASM_API_VERSION, superVisitor);
        this.method = method;
        this.descriptor = descriptor;
        this.enclosingClassDescriptor = enclosingClassDescriptor;
        this.isEntity = isEntity;
    }

    @Override
    public void visitEnd() {
        // inject a delegating body
        AsmUtil.copyParameterNames(this, method);
        visitCode();
        // inject a first argument EntityManager
        if (!isEntity) {
            visitVarInsn(Opcodes.ALOAD, 0);
        }
        visitMethodInsn(isEntity ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, enclosingClassDescriptor,
                "getEntityManager", "()" + ENTITY_MANAGER_SIGNATURE, false);
        // copy the rest of the arguments
        int index = isEntity ? 0 : 1;
        for (Type parameterType : method.parameterTypes()) {
            visitVarInsn(AsmUtil.getLoadOpcode(parameterType), index);
            index += AsmUtil.getParameterSize(parameterType);
        }
        // inject a first argument in the descriptor
        String descriptorWithEntityManager = "(" + ENTITY_MANAGER_SIGNATURE + descriptor.substring(1);
        String owner = method.declaringClass().name().toString('/') + "_";
        visitMethodInsn(Opcodes.INVOKESTATIC, owner, method.name(), descriptorWithEntityManager, false);

        // return
        int lastParen = descriptor.lastIndexOf(')');
        String returnDescriptor = descriptor.substring(lastParen + 1);
        int returnInstruction = AsmUtil.getReturnInstruction(returnDescriptor);
        visitInsn(returnInstruction);

        visitMaxs(0, 0);
        super.visitEnd();
    }
}
