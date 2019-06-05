package io.quarkus.example.panache;

import javax.persistence.Entity;

import io.quarkus.panache.rx.PanacheRxEntity;

@Entity
public class RxDataTypeEntity extends PanacheRxEntity<RxDataTypeEntity> {

    public boolean primitiveBoolean;
    public Boolean boxedBoolean;

    public char primitiveCharacter;
    public Character boxedCharacter;

    public byte primitiveByte;
    public Byte boxedByte;

    public short primitiveShort;
    public Short boxedShort;

    public int primitiveInteger;
    public Integer boxedInteger;

    public long primitiveLong;
    public Long boxedLong;

    public float primitiveFloat;
    public Float boxedFloat;

    public double primitiveDouble;
    public Double boxedDouble;
    
    public String string;
//
//    public BigInteger bigInteger;
//    public BigDecimal bigDecimal;
//
//    // date
//    public java.sql.Date sqlDate;
//    // time
//    public java.sql.Time sqlTime;
//    // timestamp
//    public java.sql.Timestamp sqlTimestamp;
//    
//    // date
//    public java.util.Date utilDate;
//    // timestamp or date
//    public java.util.Calendar utilCalendar;
//
//    // bigint
//    public Duration duration;
//    // timestamp
//    public Instant instant;
//    // timestamp
//    public LocalDateTime localDateTime;
//    // date
//    public LocalDate localDate;
//    // time
//    public LocalTime localTime;
//    // timestamp
//    public OffsetDateTime offsetDateTime;
//    // time
//    public OffsetTime offsetTime;
//    // timestamp
//    public ZonedDateTime zonedDateTime;
//    
//    public UUID uuid;
    
    // FIXME: byte[], Byte[], char[], Character[]
    
    // FIXME: enum, Serializable?
    // FIXME: Blob, Clob
    // FIXME: Embedded?, ManyToMany, arrays, Maps, Collections
}
