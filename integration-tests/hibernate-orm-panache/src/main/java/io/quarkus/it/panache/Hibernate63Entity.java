package io.quarkus.it.panache;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Hibernate63Entity extends PanacheEntity {
    public String field;

    @Find
    public static native Hibernate63Entity findByField(String field);

    @HQL("where field = :field")
    public static native Hibernate63Entity findByFieldHql(String field);

    @SQL("select * from Hibernate63Entity where field = :field")
    public static native Hibernate63Entity findByFieldSql(String field);

    @HQL("delete from Hibernate63Entity where field = :field")
    public static native int deleteField(String field);

    @HQL("update Hibernate63Entity set field = :newValue where field = :oldValue")
    public static native void updateField(String oldValue, String newValue);
}
