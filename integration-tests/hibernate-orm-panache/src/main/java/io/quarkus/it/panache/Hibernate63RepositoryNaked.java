package io.quarkus.it.panache;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

public interface Hibernate63RepositoryNaked {

    @Find
    public Hibernate63Entity findByField(String field);

    @HQL("where field = :field")
    public Hibernate63Entity findByFieldHql(String field);

    @SQL("select * from Hibernate63Entity where field = :field")
    public Hibernate63Entity findByFieldSql(String field);

    @HQL("delete from Hibernate63Entity where field = :field")
    public int deleteField(String field);

    @HQL("update Hibernate63Entity set field = :newValue where field = :oldValue")
    public void updateField(String oldValue, String newValue);
}
