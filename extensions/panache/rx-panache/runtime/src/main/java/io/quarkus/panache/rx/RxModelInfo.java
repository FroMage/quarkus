package io.quarkus.panache.rx;

import java.util.concurrent.CompletionStage;

import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

public interface RxModelInfo<T extends PanacheRxEntityBase<?>> {

    Class<T> getEntityClass();

    String getTableName();

    T fromRow(Row row);

    String insertStatement();

    String updateStatement();

    CompletionStage<Tuple> toTuple(T entity);

    CompletionStage<Void> afterSave(T entity);

    CompletionStage<Void> beforeDelete(T entity);

    Object getId(T entity);

    void setId(T entity, Object id);

    boolean isPersistent(T entity);

    boolean isGeneratedId();

    String getGeneratorSequence();

    String getIdName();

    void markPersistent(T entity);
}