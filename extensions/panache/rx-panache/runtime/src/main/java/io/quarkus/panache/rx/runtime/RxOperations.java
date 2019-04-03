package io.quarkus.panache.rx.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.quarkus.arc.Arc;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.PanacheRxQuery;
import io.quarkus.panache.rx.RxModelInfo;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.axle.pgclient.Tuple;

public class RxOperations {

    //
    // instance methods

    public static <T extends PanacheRxEntityBase<?>> CompletionStage<? extends T> save(T entity) {
        PgPool pool = getPgPool();
        @SuppressWarnings("unchecked")
        RxModelInfo<T> modelInfo = (RxModelInfo<T>) entity.getModelInfo();
        // FIXME: custom id generation
        return modelInfo.toTuple(entity).thenCompose(t -> {
            if (entity._getId() == null)
                return pool.preparedQuery("SELECT nextval('hibernate_sequence') AS id")
                        .thenApply(rowset -> rowset.iterator().next().getLong("id")).thenCompose(id -> {
                            // non-persisted tuples are missing their id
                            Tuple withId = Tuple.tuple();
                            withId.addValue(id);
                            for (int i = 0; i < t.size(); i++) {
                                withId.addValue(t.getValue(i));
                            }
                            return pool.preparedQuery(modelInfo.insertStatement(), withId).thenApply(rowset -> {
                                entity._setId(id);
                                return entity;
                            });
                        });
            else
                return pool.preparedQuery(modelInfo.updateStatement(), t).thenApply(rowset -> entity);
        });
    }

    public static CompletionStage<Void> save(Iterable<? extends PanacheRxEntityBase<?>> entities) {
        CompletionStage<?> ret = CompletableFuture.completedFuture(null);
        for (PanacheRxEntityBase<?> entity : entities) {
            ret = ret.thenCompose(v -> entity.save());
        }
        return ret.thenApply(v -> null);
    }

    public static CompletionStage<Void> save(PanacheRxEntityBase<?> firstEntity, PanacheRxEntityBase<?>... entities) {
        CompletionStage<?> ret = firstEntity.save();
        for (PanacheRxEntityBase<?> entity : entities) {
            ret = ret.thenCompose(v -> entity.save());
        }
        return ret.thenApply(v -> null);
    }

    public static <T extends PanacheRxEntityBase<?>> CompletionStage<Void> delete(T entity) {
        PgPool pool = getPgPool();
        // FIXME: id column from model info
        return pool.preparedQuery("DELETE FROM " + entity.getModelInfo().getTableName() + " WHERE id = $1", Tuple.of(entity._getId()))
                // ignoreElement
                .thenApply(rowset -> null);
    }

    // Used by generated model
    public static <T> CompletionStage<T> deferCompletionStage(Callable<CompletionStage<T>> csSource) {
        return new LazyCompletionStage<T>(csSource);
    }

    // Used by generated model
    public static <T> Publisher<T> deferPublisher(Callable<Publisher<T>> publisherSource) {
        return new LazyPublisher<T>(publisherSource);
    }

    // Used by generated model
    public static <T, R> CompletionStage<R> zipArray(Function<? super Object[], ? extends R> zipper,
                                                     @SuppressWarnings("unchecked") CompletionStage<? extends T>... sources) {
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

    public static boolean isPersistent(PanacheRxEntityBase<?> entity) {
        return entity._getId() != null;
    }

    //
    // Private stuff

    public static PgPool getPgPool() {
        io.reactiverse.pgclient.PgPool pgPool = Arc.container().instance(io.reactiverse.pgclient.PgPool.class).get();
        return PgPool.newInstance(pgPool);
    }

    public static Tuple bindParameters(Object[] params) {
        if (params == null || params.length == 0)
            return Tuple.tuple();
        Tuple t = Tuple.tuple();
        for (int i = 0; i < params.length; i++) {
            t.addValue(params[i]);
        }
        return t;
    }

    public static Tuple bindParameters(Map<String, Object> params) {
        if (params == null || params.size() == 0)
            return Tuple.tuple();
        // FIXME: turn named params into ordered params
        return Tuple.tuple();
    }

    private static int paramCount(Object[] params) {
        return params != null ? params.length : 0;
    }

    private static int paramCount(Map<String, Object> params) {
        return params != null ? params.size() : 0;
    }

    private static String getEntityName(RxModelInfo<?> modelInfo) {
        return modelInfo.getTableName();
    }

    private static String createFindQuery(RxModelInfo<?> modelInfo, String query, int paramCount) {
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
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            translatedQuery += " = $1";
        }
        return "SELECT * FROM " + getEntityName(modelInfo) + " WHERE " + translatedQuery;
    }

    private static String translateQuery(String query) {
        return query.replaceAll("\\?(\\d+)", "\\$$1");
    }

    private static String createCountQuery(RxModelInfo<?> modelInfo, String query, int paramCount) {
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
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            translatedQuery += " = $1";
        }
        return "SELECT COUNT(*) FROM " + getEntityName(modelInfo) + " WHERE " + translatedQuery;
    }

    private static String createDeleteQuery(RxModelInfo<?> modelInfo, String query, int paramCount) {
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
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            translatedQuery += " = $1";
        }
        return "DELETE FROM " + getEntityName(modelInfo) + " WHERE " + translatedQuery;
    }

    //
    // Static Helpers

    public static CompletionStage<?> findById(RxModelInfo<?> modelInfo, Object id) {
        PgPool pool = getPgPool();
        // FIXME: field list and id column name from model info
        return pool.preparedQuery("SELECT * FROM " + modelInfo.getTableName() + " WHERE id = $1", Tuple.of(id)).thenApply(rowset -> {
            if (rowset.size() == 1)
                return modelInfo.fromRow(rowset.iterator().next());
            return null;
        });
    }

    public static PanacheRxQuery<?> find(RxModelInfo<?> modelInfo, String query, Object... params) {
        return find(modelInfo, query, null, params);
    }

    public static PanacheRxQuery<?> find(RxModelInfo<?> modelInfo, String query, Sort sort, Object... params) {
        String findQuery = createFindQuery(modelInfo, query, paramCount(params));
        PgPool pool = getPgPool();
        // FIXME: check for duplicate ORDER BY clause?
        String sortedQuery = sort != null ? findQuery + sort.toOrderBy() : findQuery;
        Tuple tuple = bindParameters(params);
        return new PanacheRxQueryImpl<>(pool, modelInfo, findQuery, sortedQuery, tuple);
    }

    public static PanacheRxQuery<?> find(RxModelInfo<?> modelInfo, String query, Map<String, Object> params) {
        return find(modelInfo, query, null, params);
    }

    public static PanacheRxQuery<?> find(RxModelInfo<?> modelInfo, String query, Sort sort, Map<String, Object> params) {
        String findQuery = createFindQuery(modelInfo, query, paramCount(params));
        PgPool pool = getPgPool();
        // FIXME: check for duplicate ORDER BY clause?
        String sortedQuery = sort != null ? findQuery + sort.toOrderBy() : findQuery;
        Tuple tuple = bindParameters(params);
        return new PanacheRxQueryImpl<>(pool, modelInfo, findQuery, sortedQuery, tuple);
    }

    public static PanacheRxQuery<?> find(RxModelInfo<?> modelInfo, String query, Parameters params) {
        return find(modelInfo, query, null, params);
    }

    public static PanacheRxQuery<?> find(RxModelInfo<?> modelInfo, String query, Sort sort, Parameters params) {
        return find(modelInfo, query, sort, params.map());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> list(RxModelInfo<?> modelInfo, String query, Object... params) {
        return (CompletionStage)find(modelInfo, query, params).list();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> list(RxModelInfo<?> modelInfo, String query, Sort sort, Object... params) {
        return (CompletionStage)find(modelInfo, query, sort, params).list();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> list(RxModelInfo<?> modelInfo, String query, Map<String, Object> params) {
        return (CompletionStage)find(modelInfo, query, params).list();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> list(RxModelInfo<?> modelInfo, String query, Sort sort, Map<String, Object> params) {
        return (CompletionStage)find(modelInfo, query, sort, params).list();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> list(RxModelInfo<?> modelInfo, String query, Parameters params) {
        return (CompletionStage)find(modelInfo, query, params).list();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> list(RxModelInfo<?> modelInfo, String query, Sort sort, Parameters params) {
        return (CompletionStage)find(modelInfo, query, sort, params).list();
    }

    public static Publisher<?> stream(RxModelInfo<?> modelInfo, String query, Object... params) {
        return find(modelInfo, query, params).stream();
    }

    public static Publisher<?> stream(RxModelInfo<?> modelInfo, String query, Sort sort, Object... params) {
        return find(modelInfo, query, sort, params).stream();
    }

    public static Publisher<?> stream(RxModelInfo<?> modelInfo, String query, Map<String, Object> params) {
        return find(modelInfo, query, params).stream();
    }

    public static Publisher<?> stream(RxModelInfo<?> modelInfo, String query, Sort sort, Map<String, Object> params) {
        return find(modelInfo, query, sort, params).stream();
    }

    public static Publisher<?> stream(RxModelInfo<?> modelInfo, String query, Parameters params) {
        return find(modelInfo, query, params).stream();
    }

    public static Publisher<?> stream(RxModelInfo<?> modelInfo, String query, Sort sort, Parameters params) {
        return find(modelInfo, query, sort, params).stream();
    }

    public static PanacheRxQuery<?> findAll(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        String query = "SELECT * FROM " + modelInfo.getTableName();
        return new PanacheRxQueryImpl<>(pool, modelInfo, query, query, Tuple.tuple());
    }

    public static PanacheRxQuery<?> findAll(RxModelInfo<?> modelInfo, Sort sort) {
        PgPool pool = getPgPool();
        String query = "SELECT * FROM " + modelInfo.getTableName();
        String sortedQuery = query + sort.toOrderBy();
        return new PanacheRxQueryImpl<>(pool, modelInfo, query, sortedQuery, Tuple.tuple());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> listAll(RxModelInfo<?> modelInfo) {
        return (CompletionStage)findAll(modelInfo).list();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static CompletionStage<List<?>> listAll(RxModelInfo<?> modelInfo, Sort sort) {
        return (CompletionStage)findAll(modelInfo, sort).list();
    }

    public static Publisher<?> streamAll(RxModelInfo<?> modelInfo) {
        return findAll(modelInfo).stream();
    }

    public static Publisher<?> streamAll(RxModelInfo<?> modelInfo, Sort sort) {
        return findAll(modelInfo, sort).stream();
    }

    public static CompletionStage<Long> count(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        return pool.query("SELECT COUNT(*) FROM " + modelInfo.getTableName()).thenApply(rowset -> rowset.iterator().next().getLong(0));
    }

    public static CompletionStage<Long> count(RxModelInfo<?> modelInfo, String query, Object... params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(createCountQuery(modelInfo, query, paramCount(params)), bindParameters(params))
                .thenApply(rowset -> rowset.iterator().next().getLong(0));
    }

    public static CompletionStage<Long> count(RxModelInfo<?> modelInfo, String query, Map<String,Object> params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(createCountQuery(modelInfo, query, paramCount(params)), bindParameters(params))
                .thenApply(rowset -> rowset.iterator().next().getLong(0));
    }

    public static CompletionStage<Long> count(RxModelInfo<?> modelInfo, String query, Parameters params) {
        return count(modelInfo, query, params.map());
    }

    public static CompletionStage<Long> deleteAll(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        return pool.query("DELETE FROM " + modelInfo.getTableName()).thenApply(rowset -> (long) rowset.rowCount());
    }

    public static CompletionStage<Long> delete(RxModelInfo<?> modelInfo, String query, Object... params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(createDeleteQuery(modelInfo, query, paramCount(params)), bindParameters(params))
                .thenApply(rowset -> (long) rowset.rowCount());
    }

    public static CompletionStage<Long> delete(RxModelInfo<?> modelInfo, String query, Map<String,Object> params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(createDeleteQuery(modelInfo, query, paramCount(params)), bindParameters(params))
                .thenApply(rowset -> (long) rowset.rowCount());
    }

    public static CompletionStage<Long> delete(RxModelInfo<?> modelInfo, String query, Parameters params) {
        return delete(modelInfo, query, params.map());
    }
    
    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }
    
    public static CompletionStage<Long> executeUpdate(String query, Object... params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(query, bindParameters(params))
                .thenApply(rowset -> (long)rowset.rowCount());
    }

    public static CompletionStage<Long> executeUpdate(String query, Map<String, Object> params) {
        PgPool pool = getPgPool();
        return pool.preparedQuery(query, bindParameters(params))
                .thenApply(rowset -> (long)rowset.rowCount());
    }

}
