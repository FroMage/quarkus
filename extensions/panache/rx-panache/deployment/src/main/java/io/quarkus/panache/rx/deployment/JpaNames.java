package io.quarkus.panache.rx.deployment;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;
import javax.persistence.Temporal;

import org.jboss.jandex.DotName;

public class JpaNames {
    static final DotName DOTNAME_STRING = DotName.createSimple(String.class.getName());
    static final DotName DOTNAME_BIGDECIMAL = DotName.createSimple(BigDecimal.class.getName());
    static final DotName DOTNAME_BIGINTEGER = DotName.createSimple(BigInteger.class.getName());

    static final DotName DOTNAME_SQL_DATE = DotName.createSimple(java.sql.Date.class.getName());
    static final DotName DOTNAME_SQL_TIME = DotName.createSimple(java.sql.Time.class.getName());
    static final DotName DOTNAME_SQL_TIMESTAMP = DotName.createSimple(java.sql.Timestamp.class.getName());
    static final DotName DOTNAME_UTIL_DATE = DotName.createSimple(java.util.Date.class.getName());
    static final DotName DOTNAME_UTIL_CALENDAR = DotName.createSimple(java.util.Calendar.class.getName());

    static final DotName DOTNAME_LOCALTIME = DotName.createSimple(java.time.LocalTime.class.getName());
    static final DotName DOTNAME_LOCALDATE = DotName.createSimple(java.time.LocalDate.class.getName());
    static final DotName DOTNAME_LOCALDATETIME = DotName.createSimple(java.time.LocalDateTime.class.getName());
    static final DotName DOTNAME_OFFSETTIME = DotName.createSimple(java.time.OffsetTime.class.getName());
    static final DotName DOTNAME_OFFSETDATETIME = DotName.createSimple(java.time.OffsetDateTime.class.getName());

    static final DotName DOTNAME_PRIMITIVE_BYTE_ARRAY = DotName.createSimple(byte[].class.getName());
    static final DotName DOTNAME_BOXED_BYTE_ARRAY = DotName.createSimple(Byte[].class.getName());
    static final DotName DOTNAME_PRIMITIVE_CHAR_ARRAY = DotName.createSimple(char[].class.getName());
    static final DotName DOTNAME_BOXED_CHAR_ARRAY = DotName.createSimple(Character[].class.getName());

    static final DotName DOTNAME_BOXED_BOOLEAN = DotName.createSimple(Boolean.class.getName());
    static final DotName DOTNAME_BOOLEAN = DotName.createSimple(Boolean.TYPE.getName());
    static final DotName DOTNAME_BOXED_BYTE = DotName.createSimple(Byte.class.getName());
    static final DotName DOTNAME_BYTE = DotName.createSimple(Byte.TYPE.getName());
    static final DotName DOTNAME_BOXED_SHORT = DotName.createSimple(Short.class.getName());
    static final DotName DOTNAME_SHORT = DotName.createSimple(Short.TYPE.getName());
    static final DotName DOTNAME_BOXED_INTEGER = DotName.createSimple(Integer.class.getName());
    static final DotName DOTNAME_INTEGER = DotName.createSimple(Integer.TYPE.getName());
    static final DotName DOTNAME_BOXED_LONG = DotName.createSimple(Long.class.getName());
    static final DotName DOTNAME_LONG = DotName.createSimple(Long.TYPE.getName());
    static final DotName DOTNAME_BOXED_FLOAT = DotName.createSimple(Float.class.getName());
    static final DotName DOTNAME_FLOAT = DotName.createSimple(Float.TYPE.getName());
    static final DotName DOTNAME_BOXED_DOUBLE = DotName.createSimple(Double.class.getName());
    static final DotName DOTNAME_DOUBLE = DotName.createSimple(Double.TYPE.getName());
    static final DotName DOTNAME_BOXED_CHARACTER = DotName.createSimple(Character.class.getName());
    static final DotName DOTNAME_CHARACTER = DotName.createSimple(Character.TYPE.getName());

    static final DotName DOTNAME_ENUMERATED = DotName.createSimple(Enumerated.class.getName());
    static final DotName DOTNAME_TEMPORAL = DotName.createSimple(Temporal.class.getName());
    static final DotName DOTNAME_COLUMN = DotName.createSimple(Column.class.getName());
    static final DotName DOTNAME_ONE_TO_MANY = DotName.createSimple(OneToMany.class.getName());
    static final DotName DOTNAME_ONE_TO_ONE = DotName.createSimple(OneToOne.class.getName());
    static final DotName DOTNAME_MANY_TO_ONE = DotName.createSimple(ManyToOne.class.getName());
    static final DotName DOTNAME_MANY_TO_MANY = DotName.createSimple(ManyToMany.class.getName());
    static final DotName DOTNAME_JOIN_TABLE = DotName.createSimple(JoinTable.class.getName());
    static final DotName DOTNAME_GENERATED_VALUE = DotName.createSimple(GeneratedValue.class.getName());
    static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());
    static final DotName DOTNAME_SEQUENCE_GENERATORS = DotName.createSimple(SequenceGenerators.class.getName());
    static final DotName DOTNAME_SEQUENCE_GENERATOR = DotName.createSimple(SequenceGenerator.class.getName());
}
