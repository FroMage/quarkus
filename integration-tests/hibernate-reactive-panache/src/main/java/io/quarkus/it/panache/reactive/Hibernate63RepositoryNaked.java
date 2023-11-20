package io.quarkus.it.panache.reactive;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import io.smallrye.mutiny.Uni;

public interface Hibernate63RepositoryNaked {

    @Find
    public Uni<Hibernate63Entity> findByField(String field);

    @HQL("where field = :field")
    public Uni<Hibernate63Entity> findByFieldHql(String field);

    @SQL("select * from Hibernate63Entity where field = :field")
    public Uni<Hibernate63Entity> findByFieldSql(String field);

    @HQL("delete from Hibernate63Entity where field = :field")
    public Uni<Integer> deleteField(String field);

    @HQL("update Hibernate63Entity set field = :newValue where field = :oldValue")
    public Uni<Void> updateField(String oldValue, String newValue);
}
