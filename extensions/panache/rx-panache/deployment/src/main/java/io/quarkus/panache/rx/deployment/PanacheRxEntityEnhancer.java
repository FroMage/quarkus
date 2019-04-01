package io.quarkus.panache.rx.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.rx.PanacheRxEntity;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.quarkus.panache.rx.runtime.RxOperations;

public class PanacheRxEntityEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String RX_ENTITY_BASE_NAME = PanacheRxEntityBase.class.getName();
    public final static String RX_ENTITY_BASE_BINARY_NAME = RX_ENTITY_BASE_NAME.replace('.', '/');
    public final static String RX_ENTITY_BASE_SIGNATURE = "L" + RX_ENTITY_BASE_BINARY_NAME + ";";

    public final static String RX_OPERATIONS_NAME = RxOperations.class.getName();
    public final static String RX_OPERATIONS_BINARY_NAME = RX_OPERATIONS_NAME.replace('.', '/');
    public final static String RX_OPERATIONS_SIGNATURE = "L" + RX_OPERATIONS_BINARY_NAME + ";";

    public final static String RX_MODEL_INFO_NAME = RxModelInfo.class.getName();
    public final static String RX_MODEL_INFO_BINARY_NAME = RX_MODEL_INFO_NAME.replace('.', '/');
    public final static String RX_MODEL_INFO_SIGNATURE = "L" + RX_MODEL_INFO_BINARY_NAME + ";";

    public final static String RX_MODEL_NAME = PanacheRxEntity.class.getName();
    public final static String RX_MODEL_BINARY_NAME = RX_MODEL_NAME.replace('.', '/');
    public final static String RX_MODEL_SIGNATURE = "L" + RX_MODEL_BINARY_NAME + ";";

    public final static String RX_MODEL_FIELD_NAME = "INSTANCE";
    public final static String RX_MODEL_SUFFIX = "$__MODEL";

    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());
    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());
    private static final DotName DOTNAME_MANY_TO_ONE = DotName.createSimple(ManyToOne.class.getName());
    private static final DotName DOTNAME_ONE_TO_MANY = DotName.createSimple(OneToMany.class.getName());
    final Map<String, EntityModel> entities = new HashMap<>();
    private IndexView index;

    public PanacheRxEntityEnhancer(IndexView index) {
        this.index = index;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor, entities);
    }

    static class ModelEnhancingClassVisitor extends ClassVisitor {

        private String thisName;
        private boolean defaultConstructorPresent;
        // set of name + "/" + descriptor (only for suspected accessor names)
        private Set<String> methods = new HashSet<>();
        private Map<String, EntityField> fields;
        private Map<String, EntityModel> entities;
        private String thisBinaryName;
        private String superName;
        private String modelName;
        private String modelBinaryName;
        private String modelDesc;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                Map<String, EntityModel> entities) {
            super(Opcodes.ASM6, outputClassVisitor);
            thisName = className;
            thisBinaryName = className.replace('.', '/');
            this.entities = entities;
            EntityModel entityModel = entities.get(className);
            this.fields = entityModel != null ? entityModel.fields : null;

            // model field
            modelName = thisName + RX_MODEL_SUFFIX;
            modelBinaryName = modelName.replace('.', '/');
            modelDesc = "L" + modelBinaryName + ";";
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.superName = superName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            if ("<init>".equals(methodName) && "()V".equals(descriptor))
                defaultConstructorPresent = true;
            if (methodName.startsWith("get")
                    || methodName.startsWith("set")
                    || methodName.startsWith("is"))
                methods.add(methodName + "/" + descriptor);
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheRxFieldAccessMethodVisitor(superVisitor, thisBinaryName, methodName, descriptor,
                    entities);
        }

        @Override
        public void visitEnd() {
            // no-arg constructor 
            MethodVisitor mv;
            if (!defaultConstructorPresent) {
                mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                        "<init>",
                        "()V",
                        null,
                        null);
                mv.visitCode();
                mv.visitIntInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        superName.replace('.', '/'),
                        "<init>",
                        "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            // getModelInfo
            mv = super.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_SYNTHETIC,
                    "getModelInfo",
                    "()" + RX_MODEL_INFO_SIGNATURE + "",
                    "()L" + RX_ENTITY_BASE_BINARY_NAME + "<+" + RX_ENTITY_BASE_SIGNATURE + ">;",
                    null);
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, modelBinaryName, RX_MODEL_FIELD_NAME, modelDesc);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

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

            generateAccessors();

            super.visitEnd();

        }

        private void generateMethod(String name, 
                                    String descriptor, 
                                    String signature, 
                                    int returnOperation, 
                                    String... parameters) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                                                 name,
                                                 descriptor,
                                                 signature,
                                                 null);
            for (int i = 0; i < parameters.length; i++) {
                mv.visitParameter(parameters[i], 0 /* modifiers */);
            }
            mv.visitCode();
            // inject model
            mv.visitFieldInsn(Opcodes.GETSTATIC, modelBinaryName, RX_MODEL_FIELD_NAME, modelDesc);
            for (int i = 0; i < parameters.length; i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i);
            }
            // inject model
            String forwardingDescriptor = "(" + RX_MODEL_INFO_SIGNATURE + descriptor.substring(1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               RX_OPERATIONS_BINARY_NAME,
                               name,
                               forwardingDescriptor, false);
            mv.visitInsn(returnOperation);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private void generateAccessors() {
            if (fields == null)
                return;
            for (EntityField field : fields.values()) {
                // Getter
                String getterName = field.getGetterName();
                String getterDescriptor = "()" + field.typeDescriptor;
                if (!methods.contains(getterName + "/" + getterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                            getterName, getterDescriptor, null, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, thisBinaryName, field.name, field.typeDescriptor);
                    int returnCode;
                    switch (field.typeDescriptor) {
                        case "Z":
                        case "B":
                        case "C":
                        case "S":
                        case "I":
                            returnCode = Opcodes.IRETURN;
                            break;
                        case "J":
                            returnCode = Opcodes.LRETURN;
                            break;
                        case "F":
                            returnCode = Opcodes.FRETURN;
                            break;
                        case "D":
                            returnCode = Opcodes.DRETURN;
                            break;
                        default:
                            returnCode = Opcodes.ARETURN;
                            break;
                    }
                    mv.visitInsn(returnCode);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                // Setter
                String setterName = field.getSetterName();
                String setterDescriptor = "(" + field.typeDescriptor + ")V";
                if (!methods.contains(setterName + "/" + setterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                            setterName, setterDescriptor, null, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    int loadCode;
                    switch (field.typeDescriptor) {
                        case "Z":
                        case "B":
                        case "C":
                        case "S":
                        case "I":
                            loadCode = Opcodes.ILOAD;
                            break;
                        case "J":
                            loadCode = Opcodes.LLOAD;
                            break;
                        case "F":
                            loadCode = Opcodes.FLOAD;
                            break;
                        case "D":
                            loadCode = Opcodes.DLOAD;
                            break;
                        default:
                            loadCode = Opcodes.ALOAD;
                            break;
                    }
                    mv.visitIntInsn(loadCode, 1);
                    mv.visitFieldInsn(Opcodes.PUTFIELD, thisBinaryName, field.name, field.typeDescriptor);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
            }
        }
    }

    public void collectFields(ClassInfo classInfo) {
        // FIXME: IMPORTANT: preserve order to keep ID field first
        Map<String, EntityField> fields = new LinkedHashMap<>();
        EntityField idField = null;
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                if (fieldInfo.hasAnnotation(DOTNAME_MANY_TO_ONE)) {
                    // FIXME: that stinks
                    Type entityType = fieldInfo.type().asParameterizedType().arguments().get(0);
                    fields.put(name, new EntityField(name, fieldInfo.type(), index, entityType));
                } else if (fieldInfo.hasAnnotation(DOTNAME_ONE_TO_MANY)) {
                    // FIXME: that stinks
                    Type entityType = fieldInfo.type().asParameterizedType().arguments().get(0);
                    AnnotationInstance oneToMany = fieldInfo.annotation(DOTNAME_ONE_TO_MANY);
                    fields.put(name,
                            new EntityField(name, fieldInfo.type(), index, entityType, oneToMany.value("mappedBy").asString()));
                } else {
                    EntityField field = new EntityField(name, fieldInfo.type(), index);
                    if (fieldInfo.hasAnnotation(DOTNAME_ID)) {
                        idField = field;
                    }
                    fields.put(name, field);
                }
            }
        }
        entities.put(classInfo.name().toString(), new EntityModel(classInfo, fields, idField));
    }
}
