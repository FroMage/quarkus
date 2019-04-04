package io.quarkus.panache.rx.deployment;

import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_MODEL_FIELD_NAME;
import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_MODEL_INFO_SIGNATURE;
import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_OPERATIONS_BINARY_NAME;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

import io.quarkus.panache.rx.PanacheRxRepository;
import io.quarkus.panache.rx.PanacheRxRepositoryBase;

public class PanacheRxRepositoryEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String PANACHE_RX_REPOSITORY_BASE_NAME = PanacheRxRepositoryBase.class.getName();
    public final static String PANACHE_RX_REPOSITORY_BASE_BINARY_NAME = PANACHE_RX_REPOSITORY_BASE_NAME.replace('.', '/');
    public final static String PANACHE_RX_REPOSITORY_BASE_SIGNATURE = "L" + PANACHE_RX_REPOSITORY_BASE_BINARY_NAME + ";";

    public final static String PANACHE_RX_REPOSITORY_NAME = PanacheRxRepository.class.getName();
    public final static String PANACHE_RX_REPOSITORY_BINARY_NAME = PANACHE_RX_REPOSITORY_NAME.replace('.', '/');
    public final static String PANACHE_RX_REPOSITORY_SIGNATURE = "L" + PANACHE_RX_REPOSITORY_BINARY_NAME + ";";

    private ClassInfo panacheRxRepositoryBaseClassInfo;

    public PanacheRxRepositoryEnhancer(IndexView index) {
        panacheRxRepositoryBaseClassInfo = index.getClassByName(PanacheRxResourceProcessor.DOTNAME_PANACHE_RX_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new RxDaoEnhancingClassVisitor(className, outputClassVisitor, panacheRxRepositoryBaseClassInfo);
    }

    static class RxDaoEnhancingClassVisitor extends ClassVisitor {

        private String entityBinaryType;
        private String rxModelInfoType;
        private String rxModelInfoDesc;
        private ClassInfo panacheRxRepositoryBaseClassInfo;
        private String entitySignature;

        public RxDaoEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRxRepositoryBaseClassInfo) {
            super(Opcodes.ASM6, outputClassVisitor);
            this.panacheRxRepositoryBaseClassInfo = panacheRxRepositoryBaseClassInfo;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            SignatureReader signatureReader = new SignatureReader(signature);
            DaoTypeFetcher daoTypeFetcher = new DaoTypeFetcher(PANACHE_RX_REPOSITORY_BINARY_NAME);
            signatureReader.accept(daoTypeFetcher);
            if (daoTypeFetcher.foundType == null) {
                daoTypeFetcher = new DaoTypeFetcher(PANACHE_RX_REPOSITORY_BASE_BINARY_NAME);
                signatureReader.accept(daoTypeFetcher);
            }

            entityBinaryType = daoTypeFetcher.foundType;
            entitySignature = "L" + entityBinaryType + ";";
            rxModelInfoType = entityBinaryType + PanacheRxEntityEnhancer.RX_MODEL_SUFFIX;
            rxModelInfoDesc = "L" + rxModelInfoType + ";";
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // FIXME: do not add method if already present 
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {

            for (MethodInfo method : panacheRxRepositoryBaseClassInfo.methods()) {
                if (method.hasAnnotation(JandexUtil.DOTNAME_GENERATE_BRIDGE))
                    generateMethod(method);
            }

            super.visitEnd();
        }

        private void generateMethod(MethodInfo method) {
            String descriptor = JandexUtil.getDescriptor(method, name -> name.equals("Entity") ? entitySignature : null);
            String signature = JandexUtil.getSignature(method, name -> name.equals("Entity") ? entitySignature : null);
            List<Type> parameters = method.parameters();

            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    method.name(),
                    descriptor,
                    signature,
                    null);
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitParameter(method.parameterName(i), 0 /* modifiers */);
            }
            mv.visitCode();
            // inject model
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i + 1);
            }
            // inject model
            String forwardingDescriptor = "(" + RX_MODEL_INFO_SIGNATURE + descriptor.substring(1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    RX_OPERATIONS_BINARY_NAME,
                    method.name(),
                    forwardingDescriptor, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

        }
    }

}
