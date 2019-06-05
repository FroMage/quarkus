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

    // FIXME: Date, math?
    // FIXME: enum, Serializable?
    // FIXME: Embedded?, ManyToMany, arrays, Maps, Collections
}
