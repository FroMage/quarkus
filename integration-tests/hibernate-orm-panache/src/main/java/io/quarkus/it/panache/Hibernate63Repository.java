package io.quarkus.it.panache;

import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class Hibernate63Repository implements PanacheRepository<Hibernate63Entity> {

    @Find
    public native Hibernate63Entity findByField(String field);

    @HQL("where field = :field")
    public native Hibernate63Entity findByFieldHql(String field);

    @SQL("select * from Hibernate63Entity where field = :field")
    public native Hibernate63Entity findByFieldSql(String field);

    @HQL("delete from Hibernate63Entity where field = :field")
    public native int deleteField(String field);

    @HQL("update Hibernate63Entity set field = :newValue where field = :oldValue")
    public native void updateField(String oldValue, String newValue);
}
