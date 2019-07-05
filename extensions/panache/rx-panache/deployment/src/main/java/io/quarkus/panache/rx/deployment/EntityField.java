package io.quarkus.panache.rx.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.JavaBeanUtil;

public class EntityField {

    String name;
    Type type;
    String typeDescriptor;
    Type entityClass;
    RelationType relationType;
    String reverseField;
    boolean isEnum;
    private String columnName;
    private SimpleTypeMapper typeMapper;
    private ClassInfo declaringClass;
    private String joinTable;
    private String joinColumn;
    private String inverseJoinColumn;
    boolean isGenerated;
    private String generator;
    boolean isId;
    EntityModel entityModel;

    public EntityField(EntityModel entityModel, FieldInfo fieldInfo, IndexView index) {
        this.entityModel = entityModel;
        this.name = fieldInfo.name();
        AnnotationInstance column = fieldInfo.annotation(JpaNames.DOTNAME_COLUMN);
        if (column != null) {
            AnnotationValue value = column.value("name");
            if (value != null)
                this.columnName = value.asString();
        }
        this.type = fieldInfo.type();
        declaringClass = fieldInfo.declaringClass();
        this.typeDescriptor = DescriptorUtils.typeToString(type);
        ClassInfo typeClass = index.getClassByName(type.name());
        // FIXME: is this right?
        this.isEnum = typeClass != null ? ((typeClass.flags() & Opcodes.ACC_ENUM) != 0) : false;

        if (fieldInfo.hasAnnotation(JpaNames.DOTNAME_MANY_TO_ONE)) {
            // FIXME: that stinks
            entityClass = fieldInfo.type().asParameterizedType().arguments().get(0);
            relationType = RelationType.MANY_TO_ONE;
        } else if (fieldInfo.hasAnnotation(JpaNames.DOTNAME_ONE_TO_ONE)) {
            // FIXME: that stinks
            entityClass = fieldInfo.type().asParameterizedType().arguments().get(0);
            relationType = RelationType.ONE_TO_ONE;
            AnnotationInstance oneToOne = fieldInfo.annotation(JpaNames.DOTNAME_ONE_TO_ONE);
            AnnotationValue mappedBy = oneToOne.value("mappedBy");
            reverseField = mappedBy != null ? mappedBy.asString() : null;
        } else if (fieldInfo.hasAnnotation(JpaNames.DOTNAME_ONE_TO_MANY)) {
            // FIXME: that stinks
            entityClass = fieldInfo.type().asParameterizedType().arguments().get(0);
            AnnotationInstance oneToMany = fieldInfo.annotation(JpaNames.DOTNAME_ONE_TO_MANY);
            relationType = RelationType.ONE_TO_MANY;
            reverseField = oneToMany.value("mappedBy").asString();
        } else if (fieldInfo.hasAnnotation(JpaNames.DOTNAME_MANY_TO_MANY)) {
            // FIXME: that stinks
            entityClass = fieldInfo.type().asParameterizedType().arguments().get(0);
            AnnotationInstance manyToMany = fieldInfo.annotation(JpaNames.DOTNAME_MANY_TO_MANY);
            relationType = RelationType.MANY_TO_MANY;
            AnnotationValue mappedBy = manyToMany.value("mappedBy");
            reverseField = mappedBy != null ? mappedBy.asString() : null;
        } else {
            typeMapper = TypeMappers.getTypeMapper(isEnum, type.name(), fieldInfo);
            if (typeMapper == null)
                throw new RuntimeException("Field type not supported yet: " + type + " for field " + name);
        }

        AnnotationInstance joinTable = fieldInfo.annotation(JpaNames.DOTNAME_JOIN_TABLE);
        if (joinTable != null) {
            AnnotationValue value = column.value("name");
            if (value != null)
                this.joinTable = value.asString();
            AnnotationValue joinColumns = column.value("joinColumns");
            if (joinColumns != null) {
                // FIXME: multiple id columns
                this.joinColumn = joinColumns.asNestedArray()[0].value("name").asString();
            }
            AnnotationValue inverseJoinColumns = column.value("inverseJoinColumns");
            if (inverseJoinColumns != null) {
                // FIXME: multiple id columns
                this.inverseJoinColumn = inverseJoinColumns.asNestedArray()[0].value("name").asString();
            }
        }
        AnnotationInstance generatedValue = fieldInfo.annotation(JpaNames.DOTNAME_GENERATED_VALUE);
        if(generatedValue != null) {
            this.isGenerated = true;
            AnnotationValue value = generatedValue.value("generator");
            this.generator = value != null ? value.asString() : "hibernate_sequence";
        }
        AnnotationInstance id = fieldInfo.annotation(JpaNames.DOTNAME_ID);
        this.isId = id != null;
    }

    public String getFromRowMethod() {
        return this.typeMapper.getFromRowMethod();
    }

    public SimpleTypeMapper getTypeMapper() {
        return this.typeMapper;
    }

    public Type mappedType() {
        // FIXME: ID type
        if (isManyToOne())
            return Type.create(JpaNames.DOTNAME_BOXED_LONG, Kind.CLASS);
        return type;
    }

    public boolean isManyToOne() {
        return relationType == RelationType.MANY_TO_ONE;
    }

    public boolean isOneToMany() {
        return relationType == RelationType.ONE_TO_MANY;
    }

    public boolean isOneToOneOwning() {
        return relationType == RelationType.ONE_TO_ONE && reverseField == null;
    }

    public boolean isOneToOneNonOwning() {
        return relationType == RelationType.ONE_TO_ONE && reverseField != null;
    }

    public boolean isManyToMany() {
        return relationType == RelationType.MANY_TO_MANY;
    }

    public String columnName() {
        if (columnName != null)
            return columnName.toLowerCase();
        // FIXME: should be locale-independent
        if (isManyToOne() || isOneToOneOwning())
            return name.toLowerCase() + "_" + getInverseEntity().getIdField().name;
        return name.toLowerCase();
    }

    public String getGetterName() {
        return JavaBeanUtil.getGetterName(name, typeDescriptor);
    }

    public String getSetterName() {
        return JavaBeanUtil.getSetterName(name);
    }

    public String typeClassName() {
        return type.name().toString();
    }

    public String entityClassName() {
        return entityClass.name().toString();
    }

    public Type entityType() {
        return entityClass;
    }

    public String mappedTypeClassName() {
        return mappedType().name().toString();
    }

    public String getToTupleStoreMethod() {
        return typeMapper == null ? null : typeMapper.getToTupleStoreMethod();
    }

    public String getToTupleStoreType() {
        if (isEnum)
            return "Ljava/lang/Enum;";
        return typeDescriptor;
    }

    public boolean isOwningRelation() {
        return relationType == RelationType.MANY_TO_ONE
                || (relationType == RelationType.MANY_TO_MANY && reverseField == null)
                || (relationType == RelationType.ONE_TO_ONE && reverseField == null);
    }

    public boolean isNonOwningRelation() {
        return relationType != null
                && !isOwningRelation();
    }

    // FIXME: only for MANY_TO_MANY?
    private EntityField getRelationOwner() {
        if (reverseField == null)
            return this;
        return getInverseRelation();
    }

    public EntityModel getInverseEntity() {
        return entityModel.modelInfo.getEntityModel(entityClass.name().toString());
    }
    
    private EntityField getInverseRelation() {
        EntityModel otherEntity = getInverseEntity();
        if(reverseField != null) {
            EntityField otherField = otherEntity.fields.get(reverseField);
            if(otherField == null)
                throw new RuntimeException("Cannot find inverse relation field to " + declaringClass + "." + name 
                                           + " in opposing entity " + entityClass + ": missing reverse field "+reverseField);
            return otherField;
        }
        for (EntityField entityField : otherEntity.fields.values()) {
            if ((entityField.isManyToMany() 
                    || entityField.isOneToMany())
                    && entityField.entityClass.name().equals(declaringClass.name()))
                return entityField;
        }
        throw new RuntimeException(
                "Cannot find inverse relation field to " + declaringClass + "." + name + " in opposing entity " + entityClass);
    }

    // FIXME: only for MANY_TO_MANY?
    public String joinTable() {
        EntityField relationOwner = getRelationOwner();
        if (relationOwner.joinTable != null)
            return relationOwner.joinTable;
        EntityModel ownerEntityModel;
        EntityModel otherEntityModel;
        if (reverseField == null) {
            ownerEntityModel = entityModel;
            otherEntityModel = getInverseEntity();
        } else {
            ownerEntityModel = getInverseEntity();
            otherEntityModel = entityModel;
        }
        return ownerEntityModel.tableName + "_" + otherEntityModel.tableName;
    }

    // FIXME: only for MANY_TO_MANY?
    public String joinColumn() {
        EntityField relationOwner = getRelationOwner();
        if (relationOwner.joinColumn != null)
            return relationOwner.joinColumn;
        if (reverseField == null) {
            EntityField inverseRelation = getInverseRelation();
            return inverseRelation.name + "_" + inverseRelation.entityModel.getIdField().name; // we're not the owner of the join column
        }
        // we're not the owner so we know the name of the join relation
        return name + "_" + entityModel.getIdField().name;
    }

    // FIXME: only for MANY_TO_MANY?
    public String inverseJoinColumn() {
        EntityField relationOwner = getRelationOwner();
        if (relationOwner.inverseJoinColumn != null)
            return relationOwner.inverseJoinColumn;
        if (reverseField == null)
            return name + "_" + entityModel.getIdField().name; // we're the owner of the inverse join column
        // we're not the owner but we know the name of the inverse join relation
        return reverseField + "_" + relationOwner.entityModel.getIdField().name;
    }

    public String generatorSequence() {
        return entityModel.modelInfo.getSequenceName(generator);
    }
}
