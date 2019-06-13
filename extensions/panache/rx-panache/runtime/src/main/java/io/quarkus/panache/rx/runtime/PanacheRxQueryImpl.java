package io.quarkus.panache.rx.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.PanacheRxQuery;
import io.quarkus.panache.rx.RxModelInfo;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.axle.pgclient.PgRowSet;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

public class PanacheRxQueryImpl<Entity extends PanacheRxEntityBase<?>> implements PanacheRxQuery<Entity> {

    private Tuple params;

    private String findQuery;

    private String sortedQuery;

    private PgPool pool;

    /*
     * We store the pageSize and apply it for each request because
     * getFirstResult() sets the page size to 1
     */
    private Page page;

    private CompletionStage<Long> count;

    private RxModelInfo<Entity> modelInfo;

    PanacheRxQueryImpl(PgPool pool, RxModelInfo<Entity> modelInfo, String findQuery, String sortedQuery, Tuple params) {
        this.pool = pool;
        this.findQuery = findQuery;
        this.sortedQuery = sortedQuery;
        this.params = params;
        this.modelInfo = modelInfo;
        page = new Page(0, Integer.MAX_VALUE);
    }

    // Builder

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheRxQuery<T> page(Page page) {
        this.page = page;
        return (PanacheRxQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheRxQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> PanacheRxQuery<T> nextPage() {
        return page(page.next());
    }

    @Override
    public <T extends Entity> PanacheRxQuery<T> previousPage() {
        return page(page.previous());
    }

    @Override
    public <T extends Entity> PanacheRxQuery<T> firstPage() {
        return page(page.first());
    }

    @Override
    public <T extends Entity> CompletionStage<PanacheRxQuery<T>> lastPage() {
        return pageCount().thenApply(pageCount -> {
            return page(page.index(pageCount - 1));
        });
    }

    @Override
    public CompletionStage<Boolean> hasNextPage() {
        return pageCount().thenApply(pageCount -> {
            return page.index < (pageCount - 1);
        });
    }

    @Override
    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    @Override
    public CompletionStage<Integer> pageCount() {
        return count().thenApply(count -> {
            if (count == 0)
                return 1; // a single page of zero results
            return (int) Math.ceil((double) count / (double) page.size);
        });
    }

    @Override
    public Page page() {
        return page;
    }

    // Results

    @Override
    public CompletionStage<Long> count() {
        if (count == null) {
            // FIXME: this is crude but good enough for a first version
            String lcQuery = findQuery.toLowerCase();
            String countQueryHql = findQuery;
            int orderByIndex = lcQuery.lastIndexOf(" order by ");
            if (orderByIndex != -1)
                countQueryHql = findQuery.substring(0, orderByIndex);
            count = pool.preparedQuery("SELECT COUNT(*) " + countQueryHql, params)
                    .thenApply(pgRowSet -> pgRowSet.iterator().next().getLong(0));
        }
        return count;
    }

    private <T extends Entity> Publisher<T> queryToEntityStream(RxModelInfo<T> modelInfo, CompletionStage<PgRowSet> rows) {
        return ReactiveStreams.fromCompletionStage(rows).flatMap(rowset -> ReactiveStreams.fromIterable(rowset.getDelegate()))
                .map(coreRow -> coreRowToEntity(modelInfo, coreRow)).buildRs();
    }

    private <T extends Entity> CompletionStage<List<T>> queryToEntityList(RxModelInfo<T> modelInfo, CompletionStage<PgRowSet> rows) {
        return rows.thenApply(rowset -> {
            List<T> ret = new ArrayList<>(rowset.size());
            for (io.reactiverse.pgclient.Row coreRow : rowset.getDelegate()) {
                ret.add(coreRowToEntity(modelInfo, coreRow));
            }
            return ret;
        });
    }

    private <T extends Entity> T coreRowToEntity(RxModelInfo<T> modelInfo, io.reactiverse.pgclient.Row coreRow) {
        try {
            Row row = Row.newInstance(coreRow);
            T t = modelInfo.fromRow(row);
            return t;
        } catch (Throwable t) {
            // just to be safe
            t.printStackTrace();
            throw t;
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Entity> CompletionStage<List<T>> list() {
        return (CompletionStage) queryToEntityList(modelInfo, pool.preparedQuery(page(this.sortedQuery), params));
    }

    private String page(String query) {
        return query + " LIMIT " + page.size + " OFFSET " + (page.index * page.size);
    }

    private String page1(String query) {
        return query + " LIMIT 1 OFFSET " + (page.index * page.size);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Publisher<T> stream() {
        return (Publisher<T>) queryToEntityStream(modelInfo, pool.preparedQuery(page(this.sortedQuery), params));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> CompletionStage<T> firstResult() {
        CompletionStage<PgRowSet> rows = pool.preparedQuery(page1(this.sortedQuery), params);
        return rows.thenApply(rowset -> {
            io.reactiverse.pgclient.PgRowSet coreRowSet = rowset.getDelegate();
            return (T) (rowset.size() > 0 ? coreRowToEntity(modelInfo, coreRowSet.iterator().next()) : null);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> CompletionStage<T> singleResult() {
        CompletionStage<PgRowSet> rows = pool.preparedQuery(page(this.sortedQuery), params);
        return rows.thenApply(rowset -> {
            io.reactiverse.pgclient.PgRowSet coreRowSet = rowset.getDelegate();
            if (rowset.size() == 0)
                throw new NoResultException();
            if (rowset.size() > 1)
                throw new NonUniqueResultException();
            return (T) coreRowToEntity(modelInfo, coreRowSet.iterator().next());
        });
    }
}
