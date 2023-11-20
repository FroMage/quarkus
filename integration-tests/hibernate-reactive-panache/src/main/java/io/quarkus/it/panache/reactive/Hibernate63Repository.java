package io.quarkus.it.panache.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class Hibernate63Repository implements PanacheRepository<Hibernate63Entity> {

    @Find
    public native Uni<Hibernate63Entity> findByField(String field);

    @HQL("where field = :field")
    public native Uni<Hibernate63Entity> findByFieldHql(String field);

    @SQL("select * from Hibernate63Entity where field = :field")
    public native Uni<Hibernate63Entity> findByFieldSql(String field);

    @HQL("delete from Hibernate63Entity where field = :field")
    public native Uni<Integer> deleteField(String field);

    @HQL("update Hibernate63Entity set field = :newValue where field = :oldValue")
    public native Uni<Void> updateField(String oldValue, String newValue);
}
