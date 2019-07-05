package io.quarkus.panache.rx.deployment;

import static io.quarkus.panache.rx.deployment.JpaNames.*;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

public class TypeMappers {
    private static final SimpleTypeMapper[] SIMPLE_ENTITY_FIELD_TYPES = new SimpleTypeMapper[] {
            new SimpleTypeMapper(DOTNAME_STRING, "getString", null, "setStringId", null),
            new SimpleTypeMapper(DOTNAME_BYTE, "getByte", null, "setByteId", "getByteId"),
            new SimpleTypeMapper(DOTNAME_BOXED_BYTE, "getBoxedByte", null, "setBoxedByteId", null),
            new SimpleTypeMapper(DOTNAME_CHARACTER, "getCharacter", "storeCharacter", "setCharacterId", "getCharacterId"),
            new SimpleTypeMapper(DOTNAME_BOXED_CHARACTER, "getBoxedCharacter", "storeBoxedCharacter"),
            new SimpleTypeMapper(DOTNAME_BOOLEAN, "getBoolean", null, "setBooleanId", "getBooleanId"),
            new SimpleTypeMapper(DOTNAME_BOXED_BOOLEAN, "getBoxedBoolean", null, "setBoxedBooleanId", null),
            new SimpleTypeMapper(DOTNAME_SHORT, "getShort", null, "setShortId", "getShortId"),
            new SimpleTypeMapper(DOTNAME_BOXED_SHORT, "getBoxedShort", null, "setBoxedShortId", null),
            new SimpleTypeMapper(DOTNAME_INTEGER, "getInteger", null, "setIntegerId", "getIntegerId"),
            new SimpleTypeMapper(DOTNAME_BOXED_INTEGER, "getBoxedInteger", null, "setBoxedIntegerId", null),
            new SimpleTypeMapper(DOTNAME_LONG, "getLong", null, "setLongId", "getLongId"),
            new SimpleTypeMapper(DOTNAME_BOXED_LONG, "getBoxedLong", null, "setBoxedLongId", null),
            new SimpleTypeMapper(DOTNAME_FLOAT, "getFloat", null, "setFloatId", "getFloatId"),
            new SimpleTypeMapper(DOTNAME_BOXED_FLOAT, "getBoxedFloat", null, "setBoxedFloatId", null),
            new SimpleTypeMapper(DOTNAME_DOUBLE, "getDouble", null, "setDoubleId", "getDoubleId"),
            new SimpleTypeMapper(DOTNAME_BOXED_DOUBLE, "getBoxedDouble", null, "setBoxedDoubleId", null),
            new SimpleTypeMapper(DOTNAME_BIGDECIMAL, "getBigDecimal", null, "setBigDecimalId", null),
            new SimpleTypeMapper(DOTNAME_BIGINTEGER, "getBigInteger", null, "setBigIntegerId", null),
            new SimpleTypeMapper(DOTNAME_SQL_DATE, "getSqlDate", "storeSqlDate", "setSqlDateId", null),
            new SimpleTypeMapper(DOTNAME_SQL_TIME, "getSqlTime", "storeSqlTime"),
            new SimpleTypeMapper(DOTNAME_SQL_TIMESTAMP, "getSqlTimestamp", "storeSqlTimestamp"),
            new SimpleTypeMapper(DOTNAME_UTIL_DATE, "getUtilDateAsTimestamp", "storeUtilDateAsTimestamp", "setUtilDateId", null) {
                public SimpleTypeMapper getMostPreciseMapper(FieldInfo fieldInfo) {
                    AnnotationInstance temporal = fieldInfo.annotation(DOTNAME_TEMPORAL);
                    if (temporal != null && temporal.value() != null) {
                        String value = temporal.value().asEnum();
                        if (value.equals("DATE"))
                            return UTIL_DATE_AS_DATE;
                        else if (value.equals("TIME"))
                            return UTIL_DATE_AS_TIME;
                    }
                    return this;
                };
            },
            new SimpleTypeMapper(DOTNAME_UTIL_CALENDAR, "getUtilCalendarAsTimestamp", "storeUtilCalendarAsTimestamp") {
                public SimpleTypeMapper getMostPreciseMapper(FieldInfo fieldInfo) {
                    AnnotationInstance temporal = fieldInfo.annotation(DOTNAME_TEMPORAL);
                    if (temporal != null && temporal.value() != null) {
                        String value = temporal.value().asEnum();
                        if (value.equals("DATE"))
                            return UTIL_CALENDAR_AS_DATE;
                        else if (value.equals("TIME"))
                            return UTIL_CALENDAR_AS_TIME;
                    }
                    return this;
                };

            },
            new SimpleTypeMapper(DOTNAME_LOCALDATE, "getLocalDate", null),
            new SimpleTypeMapper(DOTNAME_LOCALTIME, "getLocalTime", null),
            new SimpleTypeMapper(DOTNAME_LOCALDATETIME, "getLocalDateTime", null),
            new SimpleTypeMapper(DOTNAME_OFFSETTIME, "getOffsetTime", null),
            new SimpleTypeMapper(DOTNAME_OFFSETDATETIME, "getOffsetDateTime", null),
            new SimpleTypeMapper(DOTNAME_PRIMITIVE_BYTE_ARRAY, "getPrimitiveByteArray", "storePrimitiveByteArray"),
            new SimpleTypeMapper(DOTNAME_BOXED_BYTE_ARRAY, "getBoxedByteArray", "storeBoxedByteArray"),
            new SimpleTypeMapper(DOTNAME_PRIMITIVE_CHAR_ARRAY, "getPrimitiveCharArray", "storePrimitiveCharArray"),
            new SimpleTypeMapper(DOTNAME_BOXED_CHAR_ARRAY, "getBoxedCharArray", "storeBoxedCharArray"),
    };

    private static final Map<DotName, SimpleTypeMapper> SIMPLE_TYPES = new HashMap<>();
    static {
        for (SimpleTypeMapper fieldType : SIMPLE_ENTITY_FIELD_TYPES) {
            SIMPLE_TYPES.put(fieldType.getEntityFieldTypeName(), fieldType);
        }
    }

    private static final SimpleTypeMapper UTIL_DATE_AS_DATE = new SimpleTypeMapper(DOTNAME_UTIL_DATE, "getUtilDateAsDate",
            "storeUtilDateAsDate", "setUtilDateId", null);
    private static final SimpleTypeMapper UTIL_DATE_AS_TIME = new SimpleTypeMapper(DOTNAME_UTIL_DATE, "getUtilDateAsTime",
            "storeUtilDateAsTime", "setUtilDateId", null);

    private static final SimpleTypeMapper UTIL_CALENDAR_AS_DATE = new SimpleTypeMapper(DOTNAME_UTIL_CALENDAR,
            "getUtilCalendarAsDate", "storeUtilCalendarAsDate");
    private static final SimpleTypeMapper UTIL_CALENDAR_AS_TIME = new SimpleTypeMapper(DOTNAME_UTIL_CALENDAR,
            "getUtilCalendarAsTime", "storeUtilCalendarAsTime");

    private static final SimpleTypeMapper ENUM_AS_ORDINAL = new SimpleTypeMapper(null, "getEnum", "storeEnum") {
        public SimpleTypeMapper getMostPreciseMapper(FieldInfo fieldInfo) {
            AnnotationInstance enumerated = fieldInfo.annotation(DOTNAME_ENUMERATED);
            if (enumerated != null) {
                AnnotationValue value = enumerated.value();
                if (value != null && value.kind() == AnnotationValue.Kind.ENUM) {
                    String enumValue = value.asEnum();
                    if ("STRING".equals(enumValue))
                        return ENUM_AS_STRING;
                }
            }
            return this;
        }
    };
    private static final SimpleTypeMapper ENUM_AS_STRING = new SimpleTypeMapper(null, "getEnumString", "storeEnumString");

    static SimpleTypeMapper getTypeMapper(boolean isEnum, DotName forEntityFieldType, FieldInfo fieldInfo) {
        SimpleTypeMapper typeMapper;
        if (isEnum) {
            typeMapper = TypeMappers.ENUM_AS_ORDINAL;
        } else {
            typeMapper = SIMPLE_TYPES.get(forEntityFieldType);
        }
        if (typeMapper != null)
            typeMapper = typeMapper.getMostPreciseMapper(fieldInfo);
        return typeMapper;
    }
}
