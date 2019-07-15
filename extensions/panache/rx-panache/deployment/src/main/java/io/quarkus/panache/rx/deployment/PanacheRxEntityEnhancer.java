package io.quarkus.panache.rx.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;

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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.quarkus.panache.rx.runtime.RxOperations;

public class PanacheRxEntityEnhancer extends PanacheEntityEnhancer<RxMetamodelInfo> {

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

    private IndexView index;

    public PanacheRxEntityEnhancer(IndexView index) {
        super(index, PanacheRxResourceProcessor.DOTNAME_PANACHE_RX_ENTITY_BASE);
        this.index = index;
        modelInfo = new RxMetamodelInfo();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo);
    }

    static class ModelEnhancingClassVisitor extends PanacheEntityClassVisitor<RxEntityField> {

        private boolean defaultConstructorPresent;
        private String superName;
        private String modelName;
        private String modelBinaryName;
        private String modelDesc;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                RxMetamodelInfo modelInfo, ClassInfo panacheRxEntityBaseClassInfo) {
            super(className, outputClassVisitor, modelInfo, panacheRxEntityBaseClassInfo);
            // model field
            modelName = className + RX_MODEL_SUFFIX;
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
                RxEntityField entityField = fields.get(name);
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
            return super.visitMethod(access, methodName, descriptor, signature, exceptions);
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

            super.visitEnd();

        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, modelBinaryName, RX_MODEL_FIELD_NAME, modelDesc);
        }

        @Override
        protected String getModelDescriptor() {
            return RX_MODEL_INFO_SIGNATURE;
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return RX_OPERATIONS_BINARY_NAME;
        }

        @Override
        protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
            mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
        }

        @Override
        protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
            mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
        }

    }

    public void collectFields(ClassInfo classInfo) {
        RxEntityModel entityModel = new RxEntityModel(classInfo, modelInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                RxEntityField field = new RxEntityField(entityModel, fieldInfo, index);
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
