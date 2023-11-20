package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;

@Entity
public class Hibernate63Entity extends PanacheEntity {
    public String field;

    @Find
    public static native Uni<Hibernate63Entity> findByField(String field);

    @HQL("where field = :field")
    public static native Uni<Hibernate63Entity> findByFieldHql(String field);

    @SQL("select * from Hibernate63Entity where field = :field")
    public static native Uni<Hibernate63Entity> findByFieldSql(String field);

    @HQL("delete from Hibernate63Entity where field = :field")
    public static native Uni<Integer> deleteField(String field);

    @HQL("update Hibernate63Entity set field = :newValue where field = :oldValue")
    public static native Uni<Void> updateField(String oldValue, String newValue);
}
