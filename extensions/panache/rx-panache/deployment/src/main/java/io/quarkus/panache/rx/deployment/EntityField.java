package io.quarkus.panache.rx.deployment;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Temporal;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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
    private FieldInfo fieldInfo;
    private String columnName;

    private static final DotName DOTNAME_STRING = DotName.createSimple(String.class.getName());
    private static final DotName DOTNAME_BIGDECIMAL = DotName.createSimple(BigDecimal.class.getName());
    private static final DotName DOTNAME_BIGINTEGER = DotName.createSimple(BigInteger.class.getName());

    private static final DotName DOTNAME_SQL_DATE = DotName.createSimple(java.sql.Date.class.getName());
    private static final DotName DOTNAME_SQL_TIME = DotName.createSimple(java.sql.Time.class.getName());
    private static final DotName DOTNAME_SQL_TIMESTAMP = DotName.createSimple(java.sql.Timestamp.class.getName());
    private static final DotName DOTNAME_UTIL_DATE = DotName.createSimple(java.util.Date.class.getName());
    private static final DotName DOTNAME_UTIL_CALENDAR = DotName.createSimple(java.util.Calendar.class.getName());

    private static final DotName DOTNAME_LOCALTIME = DotName.createSimple(java.time.LocalTime.class.getName());
    private static final DotName DOTNAME_LOCALDATE = DotName.createSimple(java.time.LocalDate.class.getName());
    private static final DotName DOTNAME_LOCALDATETIME = DotName.createSimple(java.time.LocalDateTime.class.getName());
    private static final DotName DOTNAME_OFFSETTIME = DotName.createSimple(java.time.OffsetTime.class.getName());
    private static final DotName DOTNAME_OFFSETDATETIME = DotName.createSimple(java.time.OffsetDateTime.class.getName());

    private static final DotName DOTNAME_PRIMITIVE_BYTE_ARRAY = DotName.createSimple(byte[].class.getName());
    private static final DotName DOTNAME_BOXED_BYTE_ARRAY = DotName.createSimple(Byte[].class.getName());
    private static final DotName DOTNAME_PRIMITIVE_CHAR_ARRAY = DotName.createSimple(char[].class.getName());
    private static final DotName DOTNAME_BOXED_CHAR_ARRAY = DotName.createSimple(Character[].class.getName());

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

    private static final DotName DOTNAME_TEMPORAL = DotName.createSimple(Temporal.class.getName());
    private static final DotName DOTNAME_COLUMN = DotName.createSimple(Column.class.getName());

    public EntityField(FieldInfo fieldInfo, IndexView index) {
        this.fieldInfo = fieldInfo;
        this.name = fieldInfo.name();
        AnnotationInstance column = fieldInfo.annotation(DOTNAME_COLUMN);
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
    }

    public EntityField(FieldInfo fieldInfo, IndexView index, Type entityClass) {
        this(fieldInfo, index);
        this.relationType = RelationType.ONE;
        this.entityClass = entityClass;
    }

    public EntityField(FieldInfo fieldInfo, IndexView index, Type entityClass, String mappedBy) {
        this(fieldInfo, index);
        this.relationType = RelationType.MANY;
        this.entityClass = entityClass;
        this.reverseField = mappedBy;
    }

    public String getFromRowMethod() {
        DotName typeName = type.name();
        // FIXME: this looks like we should just default to get+typeName
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

        if (typeName.equals(DOTNAME_BIGDECIMAL))
            return "getBigDecimal";
        if (typeName.equals(DOTNAME_BIGINTEGER))
            return "getBigInteger";

        if (typeName.equals(DOTNAME_SQL_DATE))
            return "getSqlDate";
        if (typeName.equals(DOTNAME_SQL_TIME))
            return "getSqlTime";
        if (typeName.equals(DOTNAME_SQL_TIMESTAMP))
            return "getSqlTimestamp";

        if (typeName.equals(DOTNAME_UTIL_DATE)) {
            AnnotationInstance temporal = fieldInfo.annotation(DOTNAME_TEMPORAL);
            if (temporal == null)
                return "getUtilDateAsTimestamp";
            String value = temporal.value().asEnum();
            if (value.equals("DATE"))
                return "getUtilDateAsDate";
            if (value.equals("TIME"))
                return "getUtilDateAsTime";
            return "getUtilDateAsTimestamp";
        }

        if (typeName.equals(DOTNAME_UTIL_CALENDAR)) {
            AnnotationInstance temporal = fieldInfo.annotation(DOTNAME_TEMPORAL);
            if (temporal == null)
                return "getUtilCalendarAsTimestamp";
            String value = temporal.value().asEnum();
            if (value.equals("DATE"))
                return "getUtilCalendarAsDate";
            if (value.equals("TIME"))
                return "getUtilCalendarAsTime";
            return "getUtilCalendarAsTimestamp";
        }

        if (typeName.equals(DOTNAME_LOCALDATE))
            return "getLocalDate";
        if (typeName.equals(DOTNAME_LOCALTIME))
            return "getLocalTime";
        if (typeName.equals(DOTNAME_LOCALDATETIME))
            return "getLocalDateTime";
        if (typeName.equals(DOTNAME_OFFSETTIME))
            return "getOffsetTime";
        if (typeName.equals(DOTNAME_OFFSETDATETIME))
            return "getOffsetDateTime";

        if (typeName.equals(DOTNAME_PRIMITIVE_BYTE_ARRAY))
            return "getPrimitiveByteArray";
        if (typeName.equals(DOTNAME_BOXED_BYTE_ARRAY))
            return "getBoxedByteArray";
        if (typeName.equals(DOTNAME_PRIMITIVE_CHAR_ARRAY))
            return "getPrimitiveCharArray";
        if (typeName.equals(DOTNAME_BOXED_CHAR_ARRAY))
            return "getBoxedCharArray";

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
        if (columnName != null)
            return columnName.toLowerCase();
        // FIXME: should be locale-independent
        if (isManyToOne())
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
        DotName typeName = type.name();
        if (typeName.equals(DOTNAME_CHARACTER))
            return "storeCharacter";
        if (typeName.equals(DOTNAME_BOXED_CHARACTER))
            return "storeBoxedCharacter";
        if (typeName.equals(DOTNAME_SQL_DATE))
            return "storeSqlDate";
        if (typeName.equals(DOTNAME_SQL_TIME))
            return "storeSqlTime";
        if (typeName.equals(DOTNAME_SQL_TIMESTAMP))
            return "storeSqlTimestamp";
        if (typeName.equals(DOTNAME_UTIL_DATE)) {
            AnnotationInstance temporal = fieldInfo.annotation(DOTNAME_TEMPORAL);
            if (temporal == null)
                return "storeUtilDateAsTimestamp";
            String value = temporal.value().asEnum();
            if (value.equals("DATE"))
                return "storeUtilDateAsDate";
            if (value.equals("TIME"))
                return "storeUtilDateAsTime";
            return "storeUtilDateAsTimestamp";
        }
        if (typeName.equals(DOTNAME_UTIL_CALENDAR)) {
            AnnotationInstance temporal = fieldInfo.annotation(DOTNAME_TEMPORAL);
            if (temporal == null)
                return "storeUtilCalendarAsTimestamp";
            String value = temporal.value().asEnum();
            if (value.equals("DATE"))
                return "storeUtilCalendarAsDate";
            if (value.equals("TIME"))
                return "storeUtilCalendarAsTime";
            return "storeUtilCalendarAsTimestamp";
        }
        
        if (typeName.equals(DOTNAME_PRIMITIVE_BYTE_ARRAY))
            return "storePrimitiveByteArray";
        if (typeName.equals(DOTNAME_BOXED_BYTE_ARRAY))
            return "storeBoxedByteArray";
        if (typeName.equals(DOTNAME_PRIMITIVE_CHAR_ARRAY))
            return "storePrimitiveCharArray";
        if (typeName.equals(DOTNAME_BOXED_CHAR_ARRAY))
            return "storeBoxedCharArray";

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
