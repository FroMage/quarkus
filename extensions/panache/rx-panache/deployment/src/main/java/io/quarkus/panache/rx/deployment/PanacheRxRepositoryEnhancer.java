package io.quarkus.panache.rx.deployment;

import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_MODEL_FIELD_NAME;
import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_MODEL_INFO_SIGNATURE;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.rx.PanacheRxRepository;
import io.quarkus.panache.rx.PanacheRxRepositoryBase;

public class PanacheRxRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public final static String PANACHE_RX_REPOSITORY_BASE_NAME = PanacheRxRepositoryBase.class.getName();
    public final static String PANACHE_RX_REPOSITORY_BASE_BINARY_NAME = PANACHE_RX_REPOSITORY_BASE_NAME.replace('.', '/');
    public final static String PANACHE_RX_REPOSITORY_BASE_SIGNATURE = "L" + PANACHE_RX_REPOSITORY_BASE_BINARY_NAME + ";";

    public final static String PANACHE_RX_REPOSITORY_NAME = PanacheRxRepository.class.getName();
    public final static String PANACHE_RX_REPOSITORY_BINARY_NAME = PANACHE_RX_REPOSITORY_NAME.replace('.', '/');
    public final static String PANACHE_RX_REPOSITORY_SIGNATURE = "L" + PANACHE_RX_REPOSITORY_BINARY_NAME + ";";

    public PanacheRxRepositoryEnhancer(IndexView index) {
        super(index, PanacheRxResourceProcessor.DOTNAME_PANACHE_RX_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheRxRepositoryClassVisitor(className, outputClassVisitor, panacheRepositoryBaseClassInfo);
    }

    static class PanacheRxRepositoryClassVisitor extends PanacheRepositoryClassVisitor {

        private String rxModelInfoType;
        private String rxModelInfoDesc;

        public PanacheRxRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRxRepositoryBaseClassInfo) {
            super(className, outputClassVisitor, panacheRxRepositoryBaseClassInfo);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            rxModelInfoType = entityBinaryType + PanacheRxEntityEnhancer.RX_MODEL_SUFFIX;
            rxModelInfoDesc = "L" + rxModelInfoType + ";";
        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            // inject model
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
        }

        @Override
        protected String getModelDescriptor() {
            return RX_MODEL_INFO_SIGNATURE;
        }

        @Override
        protected String getPanacheRepositoryBinaryName() {
            return PANACHE_RX_REPOSITORY_BINARY_NAME;
        }

        @Override
        protected String getPanacheRepositoryBaseBinaryName() {
            return PANACHE_RX_REPOSITORY_BASE_BINARY_NAME;
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return PanacheRxEntityEnhancer.RX_OPERATIONS_BINARY_NAME;
        }
    }

}
