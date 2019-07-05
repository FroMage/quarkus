package io.quarkus.panache.rx.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Target;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.JandexUtil;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.quarkus.panache.rx.runtime.RxOperations;

public class PanacheRxEntityEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String TRANSIENT_NAME = Transient.class.getName();
    public final static String TRANSIENT_BINARY_NAME = TRANSIENT_NAME.replace('.', '/');
    public final static String TRANSIENT_SIGNATURE = "L" + TRANSIENT_BINARY_NAME + ";";

    public final static String JOIN_TABLE_NAME = JoinTable.class.getName();
    public final static String JOIN_TABLE_BINARY_NAME = JOIN_TABLE_NAME.replace('.', '/');
    public final static String JOIN_TABLE_SIGNATURE = "L" + JOIN_TABLE_BINARY_NAME + ";";

    public final static String JOIN_COLUMN_NAME = JoinColumn.class.getName();
    public final static String JOIN_COLUMN_BINARY_NAME = JOIN_COLUMN_NAME.replace('.', '/');
    public final static String JOIN_COLUMN_SIGNATURE = "L" + JOIN_COLUMN_BINARY_NAME + ";";

    public final static String TARGET_NAME = Target.class.getName();
    public final static String TARGET_BINARY_NAME = TARGET_NAME.replace('.', '/');
    public final static String TARGET_SIGNATURE = "L" + TARGET_BINARY_NAME + ";";

    public final static String RX_ENTITY_BASE_NAME = PanacheRxEntityBase.class.getName();
    public final static String RX_ENTITY_BASE_BINARY_NAME = RX_ENTITY_BASE_NAME.replace('.', '/');
    public final static String RX_ENTITY_BASE_SIGNATURE = "L" + RX_ENTITY_BASE_BINARY_NAME + ";";

    public final static String RX_OPERATIONS_NAME = RxOperations.class.getName();
    public final static String RX_OPERATIONS_BINARY_NAME = RX_OPERATIONS_NAME.replace('.', '/');
    public final static String RX_OPERATIONS_SIGNATURE = "L" + RX_OPERATIONS_BINARY_NAME + ";";

    public final static String RX_MODEL_INFO_NAME = RxModelInfo.class.getName();
    public final static String RX_MODEL_INFO_BINARY_NAME = RX_MODEL_INFO_NAME.replace('.', '/');
    public final static String RX_MODEL_INFO_SIGNATURE = "L" + RX_MODEL_INFO_BINARY_NAME + ";";

    public final static String MANY_TO_MANY_NAME = ManyToMany.class.getName();
    public final static String MANY_TO_MANY_BINARY_NAME = MANY_TO_MANY_NAME.replace('.', '/');
    public final static String MANY_TO_MANY_SIGNATURE = "L" + MANY_TO_MANY_BINARY_NAME + ";";

    public final static String RX_MODEL_FIELD_NAME = "INSTANCE";
    public final static String RX_MODEL_SUFFIX = "$__MODEL";

    public final static String RX_PERSISTENT_FIELD_NAME = "__persistent";

    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());

    ModelInfo modelInfo = new ModelInfo();
    private IndexView index;
    private ClassInfo panacheRxEntityBaseClassInfo;

    public PanacheRxEntityEnhancer(IndexView index) {
        this.index = index;
        panacheRxEntityBaseClassInfo = index.getClassByName(PanacheRxResourceProcessor.DOTNAME_PANACHE_RX_ENTITY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor, modelInfo, panacheRxEntityBaseClassInfo);
    }

    static class ModelEnhancingClassVisitor extends ClassVisitor {

        private String thisName;
        private boolean defaultConstructorPresent;
        // set of name + "/" + descriptor (only for suspected accessor names)
        private Set<String> methods = new HashSet<>();
        private Map<String, EntityField> fields;
        private String thisBinaryName;
        private String superName;
        private String modelName;
        private String modelBinaryName;
        private String modelDesc;
        private ClassInfo panacheRxEntityBaseClassInfo;
        private ModelInfo modelInfo;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                ModelInfo modelInfo, ClassInfo panacheRxEntityBaseClassInfo) {
            super(Opcodes.ASM6, outputClassVisitor);
            thisName = className;
            thisBinaryName = className.replace('.', '/');
            this.modelInfo = modelInfo;
            EntityModel entityModel = modelInfo.getEntityModel(className);
            this.fields = entityModel != null ? entityModel.fields : null;
            this.panacheRxEntityBaseClassInfo = panacheRxEntityBaseClassInfo;

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
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if ((access & Opcodes.ACC_PUBLIC) != 0 && fields != null) {
                EntityField entityField = fields.get(name);
                if (entityField != null) {
                    if (entityField.isOneToMany() || entityField.isManyToMany()) {
                        // must create a fake field for hibernate with the same annotations
                        FieldVisitor fakeFieldVisitor = super.visitField(Opcodes.ACC_PUBLIC, "__hibernate_fake_" + name,
                                "Ljava/util/List;",
                                "Ljava/util/List" + signature.substring(signature.indexOf("<")),
                                null);

                        if (entityField.isManyToMany() && entityField.isOwningRelation()) {
                            // FIXME: this is a hack
                            AnnotationVisitor annotationVisitor = fakeFieldVisitor.visitAnnotation(JOIN_TABLE_SIGNATURE, true);

                            AnnotationVisitor annotationArrayVisitor = annotationVisitor.visitArray("joinColumns");

                            AnnotationVisitor joinColumnVisitor = annotationArrayVisitor.visitAnnotation(null,
                                    JOIN_COLUMN_SIGNATURE);
                            joinColumnVisitor.visit("name", entityField.joinColumn());
                            joinColumnVisitor.visitEnd();

                            annotationArrayVisitor.visitEnd();
                            annotationArrayVisitor = annotationVisitor.visitArray("inverseJoinColumns");

                            joinColumnVisitor = annotationArrayVisitor.visitAnnotation(null, JOIN_COLUMN_SIGNATURE);
                            joinColumnVisitor.visit("name", entityField.inverseJoinColumn());
                            joinColumnVisitor.visitEnd();

                            annotationArrayVisitor.visitEnd();
                            annotationVisitor.visitEnd();
                        }

                        // must add the @Transient annotation for hibernate. we don't care about it since we already
                        // read the field
                        FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
                        AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(TRANSIENT_SIGNATURE, true);
                        annotationVisitor.visitEnd();

                        return new FieldVisitor(Opcodes.ASM6, fieldVisitor) {
                            @Override
                            public void visitAttribute(Attribute attribute) {
                                fakeFieldVisitor.visitAttribute(attribute);
                                super.visitAttribute(attribute);
                            }

                            @Override
                            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                return new MultiplexingAnnotationVisitor(fakeFieldVisitor.visitAnnotation(descriptor, visible),
                                        super.visitAnnotation(descriptor, visible)) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        if (descriptor.equals(MANY_TO_MANY_SIGNATURE) && name.equals("mappedBy"))
                                            super.visit(name, "__hibernate_fake_" + value);
                                        else
                                            super.visit(name, value);
                                    }
                                };
                            }

                            @Override
                            public void visitEnd() {
                                fakeFieldVisitor.visitEnd();
                                super.visitEnd();
                            }

                            @Override
                            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
                                    boolean visible) {
                                return new MultiplexingAnnotationVisitor(
                                        fakeFieldVisitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible),
                                        super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
                            }
                        };
                    } else if (entityField.isManyToOne() || entityField.isOneToOneNonOwning()
                            || entityField.isOneToOneOwning()) {
                        // Add @Target(class)
                        FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
                        AnnotationVisitor annotationVisitor = fieldVisitor.visitAnnotation(TARGET_SIGNATURE, true);
                        annotationVisitor.visit("value",
                                org.objectweb.asm.Type.getType(DescriptorUtils.typeToString(entityField.entityType())));
                        annotationVisitor.visitEnd();

                        return fieldVisitor;
                    }
                }
            }
            return super.visitField(access, name, descriptor, signature, value);
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
                    modelInfo);
        }

        @Override
        public void visitEnd() {

            // add our package-private persistent field
            super.visitField(Opcodes.ACC_SYNTHETIC, RX_PERSISTENT_FIELD_NAME, "Z", null, null);

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

            for (MethodInfo method : panacheRxEntityBaseClassInfo.methods()) {
                if (method.hasAnnotation(JandexUtil.DOTNAME_GENERATE_BRIDGE))
                    generateMethod(method);
            }

            generateAccessors();

            super.visitEnd();

        }

        private void generateMethod(MethodInfo method) {
            String descriptor = JandexUtil.getDescriptor(method, name -> null);
            String signature = JandexUtil.getSignature(method, name -> null);
            List<Type> parameters = method.parameters();

            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    method.name(),
                    descriptor,
                    signature,
                    null);
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitParameter(method.parameterName(i), 0 /* modifiers */);
            }
            mv.visitCode();
            // inject model
            mv.visitFieldInsn(Opcodes.GETSTATIC, modelBinaryName, RX_MODEL_FIELD_NAME, modelDesc);
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i);
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
                    int returnCode = JandexUtil.getReturnInstruction(field.typeDescriptor);
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
        EntityModel entityModel = new EntityModel(classInfo, modelInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                EntityField field = new EntityField(entityModel, fieldInfo, index);
                entityModel.addField(field);
            }
            collectSequenceGenerators(fieldInfo.annotations());
        }
        collectSequenceGenerators(classInfo.classAnnotations());
        modelInfo.addEntityModel(entityModel);
    }

    private void collectSequenceGenerators(Collection<AnnotationInstance> classAnnotations) {
        for (AnnotationInstance annotation : classAnnotations) {
            if (annotation.name().equals(JpaNames.DOTNAME_SEQUENCE_GENERATORS)) {
                for (AnnotationInstance sequenceGenerator : annotation.value().asNestedArray()) {
                    collectSequenceGenerator(sequenceGenerator);
                }
            } else if (annotation.name().equals(JpaNames.DOTNAME_SEQUENCE_GENERATOR)) {
                collectSequenceGenerator(annotation);
            }
        }
    }

    private void collectSequenceGenerator(AnnotationInstance sequenceGenerator) {
        String name = sequenceGenerator.value("name").asString();
        AnnotationValue sequenceName = sequenceGenerator.value("sequenceName");
        modelInfo.addSequenceGenerator(name, sequenceName != null ? sequenceName.asString() : name);
    }
}
