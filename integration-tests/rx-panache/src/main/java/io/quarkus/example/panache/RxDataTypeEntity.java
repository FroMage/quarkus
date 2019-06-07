package io.quarkus.example.panache;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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

    public BigInteger bigInteger;
    public BigDecimal bigDecimal;

    // date
    public java.sql.Date sqlDate;
    // time
    public java.sql.Time sqlTime;
    // timestamp
    public java.sql.Timestamp sqlTimestamp;
    
    // timestamp
    public java.util.Date utilDate;
    
    @Temporal(TemporalType.DATE)
    public java.util.Date utilDateAsDate;

    @Temporal(TemporalType.TIME)
    public java.util.Date utilDateAsTime;
    
    @Temporal(TemporalType.TIMESTAMP)
    public java.util.Date utilDateAsTimestamp;

    // timestamp
    public java.util.Calendar utilCalendar;

    @Temporal(TemporalType.DATE)
    public java.util.Calendar utilCalendarAsDate;

    // Hibernate refuses to store Calendar as TIME
    
    @Temporal(TemporalType.TIMESTAMP)
    public java.util.Calendar utilCalendarAsTimestamp;

    public LocalDateTime localDateTime;
    public LocalDate localDate;
    @Column(name = "myLocalTime")
    public LocalTime localTime;
    
    @Column(columnDefinition = "timestamp with time zone")
    public OffsetDateTime offsetDateTime;
    @Column(columnDefinition = "time with time zone")
    public OffsetTime offsetTime;

    //    
//    public UUID uuid;
    
    // FIXME: byte[], Byte[], char[], Character[]
    
    // FIXME: enum, Serializable?
    // FIXME: Blob, Clob
    // FIXME: Embedded?, ManyToMany, arrays, Maps, Collections
}
