package io.quarkus.panache.rx.runtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.persistence.Query;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.quarkus.arc.Arc;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

public class RxOperations {

    // 
    // instance methods

    public static <T extends PanacheRxEntityBase<?>> CompletionStage<? extends T> save(T entity) {
        PgPool pool = getPgPool();
        @SuppressWarnings("unchecked")
        RxModelInfo<T> modelInfo = (RxModelInfo<T>) entity.getModelInfo();
        // FIXME: custom id generation
        return modelInfo.toTuple(entity)
                .thenCompose(t -> {
                    if (entity._getId() == null)
                        return pool.preparedQuery("SELECT nextval('hibernate_sequence') AS id")
                                .thenApply(rowset -> rowset.iterator().next().getLong("id"))
                                .thenCompose(id -> {
                                    // non-persisted tuples are missing their id
                                    Tuple withId = Tuple.tuple();
                                    withId.addValue(id);
                                    for (int i = 0; i < t.size(); i++) {
                                        withId.addValue(t.getValue(i));
                                    }
                                    return pool.preparedQuery(modelInfo.insertStatement(), withId)
                                            .thenApply(rowset -> {
                                                entity._setId(id);
                                                return entity;
                                            });
                                });
                    else
                        return pool.preparedQuery(modelInfo.updateStatement(), t)
                                .thenApply(rowset -> entity);
                });
    }

    public static <T extends PanacheRxEntityBase<?>> CompletionStage<Void> delete(T entity) {
        PgPool pool = getPgPool();
        // FIXME: id column from model info
        return pool.preparedQuery("DELETE FROM " + entity.getModelInfo().getTableName() + " WHERE id = $1",
                Tuple.of(entity._getId()))
                // ignoreElement
                .thenApply(rowset -> null);
    }

    public static <T> CompletionStage<T> deferCompletionStage(Callable<CompletionStage<T>> csSource) {
        return new LazyCompletionStage<T>(csSource);
    }

    public static <T> Publisher<T> deferPublisher(Callable<Publisher<T>> publisherSource) {
        return new LazyPublisher<T>(publisherSource);
    }

    public static <T, R> CompletionStage<R> zipArray(Function<? super Object[], ? extends R> zipper,
            CompletionStage<? extends T>... sources) {
        Object[] results = new Object[sources.length];
        CompletionStage<?> state = CompletableFuture.completedFuture(null);
        for (int i = 0; i < sources.length; i++) {
            CompletionStage<? extends T> completionStage = sources[i];
            int finalI = i;
            CompletionStage<Void> stage = completionStage.thenAccept(result -> {
                results[finalI] = result;
            });
            state = state.thenCompose(v -> stage);
        }
        return state.thenApply(v -> zipper.apply(results));
    }

    //
    // Private stuff

    private static PgPool getPgPool() {
        // FIXME: make it return CompletionStage to make it deferred
        io.reactiverse.pgclient.PgPool pgPool = Arc.container().instance(io.reactiverse.pgclient.PgPool.class).get();
        return PgPool.newInstance(pgPool);
    }

    public static Query bindParameters(Query query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static Query bindParameters(Query query, Map<String, Object> params) {
        if (params == null || params.size() == 0)
            return query;
        for (Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    private static Tuple toParams(Object[] params) {
        Tuple t = Tuple.tuple();
        for (Object param : params) {
            t.addValue(param);
        }
        return t;
    }

    private static String createFindQuery(RxModelInfo<?> modelInfo, String query, Object[] params) {
        // FIXME: field order from model info
        if (query == null)
            return "SELECT * FROM " + getEntityName(modelInfo);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "SELECT * FROM " + getEntityName(modelInfo);

        String trimmedLc = trimmed.toLowerCase();
        String translatedQuery = translateQuery(query);
        if (trimmedLc.startsWith("from ")) {
            return "SELECT * " + translatedQuery;
        }
        if (trimmedLc.startsWith("select ")) {
            throw new IllegalArgumentException("Select queries not yet supported");
        }
        if (trimmedLc.startsWith("order by ")) {
            return "SELECT * FROM " + getEntityName(modelInfo) + " " + translatedQuery;
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && params != null && params.length == 1) {
            translatedQuery += " = $1";
        }
        return "SELECT * FROM " + getEntityName(modelInfo) + " WHERE " + translatedQuery;
    }

    private static String translateQuery(String query) {
        return query.replaceAll("\\?(\\d+)", "\\$$1");
    }

    private static String createCountQuery(RxModelInfo<?> modelInfo, String query, Object[] params) {
        if (query == null)
            return "SELECT COUNT(*) FROM " + getEntityName(modelInfo);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "SELECT COUNT(*) FROM " + getEntityName(modelInfo);

        String trimmedLc = trimmed.toLowerCase();
        String translatedQuery = translateQuery(query);
        if (trimmedLc.startsWith("from ")) {
            return "SELECT COUNT(*) " + translatedQuery;
        }
        if (trimmedLc.startsWith("order by ")) {
            // ignore it
            return "SELECT COUNT(*) FROM " + getEntityName(modelInfo);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && params != null && params.length == 1) {
            translatedQuery += " = $1";
        }
        return "SELECT COUNT(*) FROM " + getEntityName(modelInfo) + " WHERE " + translatedQuery;
    }

    private static String createDeleteQuery(RxModelInfo<?> modelInfo, String query, Object[] params) {
        if (query == null)
            return "DELETE FROM " + getEntityName(modelInfo);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "DELETE FROM " + getEntityName(modelInfo);

        String trimmedLc = trimmed.toLowerCase();
        String translatedQuery = translateQuery(query);
        if (trimmedLc.startsWith("from ")) {
            return "DELETE " + translatedQuery;
        }
        if (trimmedLc.startsWith("order by ")) {
            // ignore it
            return "DELETE FROM " + getEntityName(modelInfo);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && params != null && params.length == 1) {
            translatedQuery += " = $1";
        }
        return "DELETE FROM " + getEntityName(modelInfo) + " WHERE " + translatedQuery;
    }

    private static String getEntityName(RxModelInfo<?> modelInfo) {
        return modelInfo.getTableName();
    }

    //
    // Static Helpers

    public static <T extends PanacheRxEntityBase<?>> Publisher<T> findAll(RxModelInfo<T> modelInfo) {
        PgPool pool = getPgPool();
        // FIXME: field list and order by from model info
        return ReactiveStreams.fromCompletionStage(pool.query("SELECT * FROM " + modelInfo.getTableName() + " ORDER BY name"))
                .flatMap(rowset -> ReactiveStreams.fromIterable(rowset.getDelegate()))
                .map(coreRow -> {
                    try {
                        Row row = Row.newInstance(coreRow);
                        T t = modelInfo.fromRow(row);
                        return t;
                    } catch (Throwable t) {
                        t.printStackTrace();
                        return null;
                    }
                }).buildRs();
    }

    public static <T extends PanacheRxEntityBase<?>> CompletionStage<T> findById(RxModelInfo<T> modelInfo, Object id) {
        PgPool pool = getPgPool();
        // FIXME: field list and id column name from model info
        return pool.preparedQuery("SELECT * FROM " + modelInfo.getTableName() + " WHERE id = $1", Tuple.of(id))
                .thenApply(rowset -> {
                    if (rowset.size() == 1)
                        return modelInfo.fromRow(rowset.iterator().next());
                    return null;
                });
    }

    public static <T extends PanacheRxEntityBase<?>> Publisher<T> find(RxModelInfo<T> modelInfo, String query,
            Object... params) {
        PgPool pool = getPgPool();
        // FIXME: order by from model info
        return ReactiveStreams
                .fromCompletionStage(pool.preparedQuery(createFindQuery(modelInfo, query, params), toParams(params)))
                .flatMap(rowset -> ReactiveStreams.fromIterable(rowset.getDelegate()))
                .map(coreRow -> {
                    try {
                        return modelInfo.fromRow(Row.newInstance(coreRow));
                    } catch (Throwable t) {
                        t.printStackTrace();
                        return null;
                    }
                }).buildRs();
    }

    public static CompletionStage<Long> count(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        return pool.query("SELECT COUNT(*) FROM " + modelInfo.getTableName())
                .thenApply(rowset -> rowset.iterator().next().getLong(0));
    }

    public static CompletionStage<Long> count(RxModelInfo<?> modelInfo, String query, Object... params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(createCountQuery(modelInfo, query, params), toParams(params))
                .thenApply(rowset -> rowset.iterator().next().getLong(0));
    }

    public static CompletionStage<Long> deleteAll(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        return pool.query("DELETE FROM " + modelInfo.getTableName())
                .thenApply(rowset -> (long) rowset.rowCount());
    }

    public static CompletionStage<Long> delete(RxModelInfo<?> modelInfo, String query, Object... params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(createDeleteQuery(modelInfo, query, params), toParams(params))
                .thenApply(rowset -> (long) rowset.rowCount());
    }
}
