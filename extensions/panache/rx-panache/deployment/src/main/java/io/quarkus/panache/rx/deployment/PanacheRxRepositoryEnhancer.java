package io.quarkus.panache.rx.deployment;

import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_ENTITY_BASE_SIGNATURE;
import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_MODEL_FIELD_NAME;
import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_MODEL_INFO_SIGNATURE;
import static io.quarkus.panache.rx.deployment.PanacheRxEntityEnhancer.RX_OPERATIONS_BINARY_NAME;

import java.util.function.BiFunction;

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

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new RxDaoEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class RxDaoEnhancingClassVisitor extends ClassVisitor {

        private String entityBinaryType;
        private String rxModelInfoType;
        private String rxModelInfoDesc;

        public RxDaoEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
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
            // findById
            generateMethod("findById",
                           "(Ljava/lang/Object;)Lio/reactivex/Maybe;",
                           "(Ljava/lang/Object;)Lio/reactivex/Maybe<" + RX_ENTITY_BASE_SIGNATURE + ">;",
                           Opcodes.ARETURN, "id");

            // find
            generateMethod("find",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable;",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Observable<" + RX_ENTITY_BASE_SIGNATURE + ">;",
                    Opcodes.ARETURN, "query", "params");

            // findAll
            generateMethod("findAll",
                    "()Lio/reactivex/Observable;",
                    "()Lio/reactivex/Observable<" + RX_ENTITY_BASE_SIGNATURE + ">;",
                    Opcodes.ARETURN);

            // count
            generateMethod("count",
                    "()Lio/reactivex/Single;",
                    "()Lio/reactivex/Single<Ljava/lang/Long;>;",
                    Opcodes.ARETURN);

            // count
            generateMethod("count",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single<Ljava/lang/Long;>;",
                    Opcodes.ARETURN, "query", "params");

            // deleteAll
            generateMethod("deleteAll",
                    "()Lio/reactivex/Single;",
                    "()Lio/reactivex/Single<Ljava/lang/Long;>;",
                    Opcodes.ARETURN);

            // delete
            generateMethod("delete",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single;",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lio/reactivex/Single<Ljava/lang/Long;>;",
                    Opcodes.ARETURN, "query", "params");

            super.visitEnd();

        }

        private void generateMethod(String name, String descriptor, String signature, int returnOpCode, String... params) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                                                 name,
                                                 descriptor,
                                                 signature,
                                                 null);
            for (int i = 0; i < params.length; i++) {
                mv.visitParameter(params[i], 0 /* modifiers */);
            }
            mv.visitCode();
            // inject model
            mv.visitFieldInsn(Opcodes.GETSTATIC, rxModelInfoType, RX_MODEL_FIELD_NAME, rxModelInfoDesc);
            for (int i = 0; i < params.length; i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i + 1);
            }
            // inject model
            String forwardingDescriptor = "(" + RX_MODEL_INFO_SIGNATURE + descriptor.substring(1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               RX_OPERATIONS_BINARY_NAME,
                               name,
                               forwardingDescriptor, false);
            mv.visitInsn(returnOpCode);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

        }
    }

}
