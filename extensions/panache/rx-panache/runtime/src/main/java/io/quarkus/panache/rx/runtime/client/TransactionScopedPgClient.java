package io.quarkus.panache.rx.runtime.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.inject.Instance;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.axle.pgclient.PgRowSet;
import io.reactiverse.axle.pgclient.PgTransaction;
import io.reactiverse.axle.pgclient.Tuple;

public class TransactionScopedPgClient extends PgClient {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";
    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry tsr;
    private static final Object transactionKey = new Object();
    private final Instance<RequestScopedPgClientHolder> requestScopedEms;
    private final PgPool pool;
    private ThreadContext threadContext;

    public TransactionScopedPgClient(TransactionManager transactionManager,
                                     TransactionSynchronizationRegistry tsr,
                                     PgPool pool,
                                     ThreadContext threadContext,
                                     Instance<RequestScopedPgClientHolder> requestScopedEms) {
        super(null);
        this.transactionManager = transactionManager;
        this.tsr = tsr;
        this.pool = pool;
        this.threadContext = threadContext;
        this.requestScopedEms = requestScopedEms;
    }

    CompletionStage<? extends PgClient> getPgClient() {
        if (isInTransaction()) {
            PgTransaction em = (PgTransaction) tsr.getResource(transactionKey);
            if (em != null) {
                return threadContext.withContextCapture(CompletableFuture.completedFuture(em));
            }
            CompletionStage<PgTransaction> newTransaction = threadContext.withContextCapture(pool.begin());
            return newTransaction.thenApply(tr -> {
                tsr.putResource(transactionKey, tr);
                tsr.registerInterposedSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                    }

                    @Override
                    public void afterCompletion(int i) {
                        try {
                            // FIXME: async?
                            // FIXME: statuses?
                            if(transactionManager.getStatus() == Status.STATUS_ROLLEDBACK)
                                tr.rollback();
                            else
                                tr.commit();
                        } catch (SystemException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        tr.close();
                    }
                });
                return tr;
            });
        } else {
            //this will throw an exception if the request scope is not active
            //this is expected as either the request scope or an active transaction
            //is required to properly managed the PgClient lifecycle
            RequestScopedPgClientHolder requestScopedEms = this.requestScopedEms.get();
            return threadContext.withContextCapture(requestScopedEms.getOrCreatePgClient(pool));
        }
    }

    private boolean isInTransaction() {
        try {
            switch (transactionManager.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletionStage<PgRowSet> preparedBatch(String sql, List<Tuple> batch) {
        return getPgClient()
                .thenCompose(client -> client.preparedBatch(sql, batch));
    }
    
    @Override
    public CompletionStage<PgRowSet> preparedQuery(String sql) {
        return getPgClient()
                .thenCompose(client -> client.preparedQuery(sql));
    }
    
    @Override
    public CompletionStage<PgRowSet> preparedQuery(String sql, Tuple arguments) {
        return getPgClient()
                .thenCompose(client -> client.preparedQuery(sql, arguments));
    }
    
    @Override
    public CompletionStage<PgRowSet> query(String sql) {
        return getPgClient()
                .thenCompose(client -> client.query(sql));
    }
    
    @Override
    public boolean equals(Object o) {
        // TODO Auto-generated method stub
        return super.equals(o);
    }
    
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }
    
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }
}
