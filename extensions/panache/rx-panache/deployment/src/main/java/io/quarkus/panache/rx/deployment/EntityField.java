package io.quarkus.panache.rx.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.DescriptorUtils;

public class EntityField {

    String name;
    Type type;
    String typeDescriptor;
    Type entityClass;
    RelationType relationType;
    String reverseField;
    boolean isEnum;

    private static final DotName DOTNAME_STRING = DotName.createSimple(String.class.getName());
    private static final DotName DOTNAME_BOXED_BOOLEAN = DotName.createSimple(Boolean.class.getName());
    private static final DotName DOTNAME_BOOLEAN = DotName.createSimple(Boolean.TYPE.getName());
    private static final DotName DOTNAME_BOXED_BYTE = DotName.createSimple(Byte.class.getName());
    private static final DotName DOTNAME_BYTE = DotName.createSimple(Byte.TYPE.getName());
    private static final DotName DOTNAME_BOXED_SHORT = DotName.createSimple(Short.class.getName());
    private static final DotName DOTNAME_SHORT = DotName.createSimple(Short.TYPE.getName());
    private static final DotName DOTNAME_BOXED_INTEGER = DotName.createSimple(Integer.class.getName());
    private static final DotName DOTNAME_INTEGER = DotName.createSimple(Integer.TYPE.getName());
    private static final DotName DOTNAME_BOXED_LONG = DotName.createSimple(Long.class.getName());
    private static final DotName DOTNAME_LONG = DotName.createSimple(Long.TYPE.getName());
    private static final DotName DOTNAME_BOXED_FLOAT = DotName.createSimple(Float.class.getName());
    private static final DotName DOTNAME_FLOAT = DotName.createSimple(Float.TYPE.getName());
    private static final DotName DOTNAME_BOXED_DOUBLE = DotName.createSimple(Double.class.getName());
    private static final DotName DOTNAME_DOUBLE = DotName.createSimple(Double.TYPE.getName());
    private static final DotName DOTNAME_BOXED_CHARACTER = DotName.createSimple(Character.class.getName());
    private static final DotName DOTNAME_CHARACTER = DotName.createSimple(Character.TYPE.getName());

    public EntityField(String name, Type type, IndexView index) {
        this.name = name;
        this.type = type;
        this.typeDescriptor = DescriptorUtils.typeToString(type);
        ClassInfo typeClass = index.getClassByName(type.name());
        // FIXME: is this right?
        this.isEnum = typeClass != null ? ((typeClass.flags() & Opcodes.ACC_ENUM) != 0) : false;
    }

    public EntityField(String name, Type type, IndexView index, Type entityClass) {
        this(name, type, index);
        this.relationType = RelationType.ONE;
        this.entityClass = entityClass;
    }

    public EntityField(String name, Type type, IndexView index, Type entityClass, String mappedBy) {
        this(name, type, index);
        this.relationType = RelationType.MANY;
        this.entityClass = entityClass;
        this.reverseField = mappedBy;
    }

    public String getFromRowMethod() {
        DotName typeName = type.name();
        if (typeName.equals(DOTNAME_STRING))
            return "getString";
        if (typeName.equals(DOTNAME_BYTE))
            return "getByte";
        if (typeName.equals(DOTNAME_BOXED_BYTE))
            return "getBoxedByte";
        if (typeName.equals(DOTNAME_CHARACTER))
            return "getCharacter";
        if (typeName.equals(DOTNAME_BOXED_CHARACTER))
            return "getBoxedCharacter";
        if (typeName.equals(DOTNAME_BOOLEAN))
            return "getBoolean";
        if (typeName.equals(DOTNAME_BOXED_BOOLEAN))
            return "getBoxedBoolean";
        if (typeName.equals(DOTNAME_SHORT))
            return "getShort";
        if (typeName.equals(DOTNAME_BOXED_SHORT))
            return "getBoxedShort";
        if (typeName.equals(DOTNAME_INTEGER))
            return "getInteger";
        if (typeName.equals(DOTNAME_BOXED_INTEGER))
            return "getBoxedInteger";
        if (typeName.equals(DOTNAME_LONG))
            return "getLong";
        if (typeName.equals(DOTNAME_BOXED_LONG))
            return "getBoxedLong";
        if (typeName.equals(DOTNAME_FLOAT))
            return "getFloat";
        if (typeName.equals(DOTNAME_BOXED_FLOAT))
            return "getBoxedFloat";
        if (typeName.equals(DOTNAME_DOUBLE))
            return "getDouble";
        if (typeName.equals(DOTNAME_BOXED_DOUBLE))
            return "getBoxedDouble";
        throw new RuntimeException("Field type not supported yet: " + type + " for field " + name);
    }

    public Type mappedType() {
        if (isEnum)
            return Type.create(DOTNAME_BOXED_INTEGER, Kind.CLASS);
        // FIXME: ID type
        if (isManyToOne())
            return Type.create(DOTNAME_BOXED_LONG, Kind.CLASS);
        return type;
    }

    public boolean isManyToOne() {
        return relationType == RelationType.ONE;
    }

    public boolean isOneToMany() {
        return relationType == RelationType.MANY;
    }

    public String columnName() {
        if (isManyToOne())
            return name + "_id";
        return name;
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

    public String mappedTypeClassName() {
        return mappedType().name().toString();
    }

    public String getToTupleStoreMethod() {
        DotName typeName = type.name();
        if (typeName.equals(DOTNAME_CHARACTER))
            return "storeCharacter";
        if (typeName.equals(DOTNAME_BOXED_CHARACTER))
            return "storeBoxedCharacter";
        if (isEnum)
            return "storeEnum";
        return null;
    }

    public String getToTupleStoreType() {
        if (isEnum)
            return "Ljava/lang/Enum;";
        return typeDescriptor;
    }
}
