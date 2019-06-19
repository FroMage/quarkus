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

    public EntityField(FieldInfo fieldInfo, IndexView index) {
        this.name = fieldInfo.name();
        AnnotationInstance column = fieldInfo.annotation(JpaNames.DOTNAME_COLUMN);
        if (column != null) {
            AnnotationValue value = column.value("name");
            if (value != null)
                this.columnName = value.asString();
        }
        this.type = fieldInfo.type();
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
        } else {
            typeMapper = TypeMappers.getTypeMapper(isEnum, type.name(), fieldInfo);
            if (typeMapper == null)
                throw new RuntimeException("Field type not supported yet: " + type + " for field " + name);
        }
    }

    public String getFromRowMethod() {
        return this.typeMapper.getFromRowMethod();
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

    public String columnName() {
        if (columnName != null)
            return columnName.toLowerCase();
        // FIXME: should be locale-independent
        if (isManyToOne() || isOneToOneOwning())
            return name.toLowerCase() + "_id";
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
                || (relationType == RelationType.ONE_TO_ONE && reverseField == null);
    }

    public boolean isNonOwningRelation() {
        return relationType != null
                && !isOwningRelation();
    }
}
