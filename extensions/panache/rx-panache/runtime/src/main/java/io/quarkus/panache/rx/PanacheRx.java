package io.quarkus.panache.rx;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.rx.runtime.RxOperations;
import io.reactiverse.axle.pgclient.PgClient;

/**
 * Utility class for Panache.
 *
 * @author Stéphane Épardaud
 */
public class PanacheRx {

    /**
     * Returns the current {@link EntityManager}
     * 
     * @return the current {@link EntityManager}
     */
    public static PgClient getPgClient() {
        return RxOperations.getPgClient();
    }

    /**
     * Returns the current {@link TransactionManager}
     * 
     * @return the current {@link TransactionManager}
     */
    public static TransactionManager getTransactionManager() {
        return RxOperations.getTransactionManager();
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     * 
     * @param query a normal HQL query
     * @param params optional list of indexed parameters
     * @return the number of rows operated on.
     */
    public static CompletionStage<Long> executeUpdate(String query, Object... params) {
        return RxOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     * 
     * @param query a normal HQL query
     * @param params {@link Map} of named parameters
     * @return the number of rows operated on.
     */
    public static CompletionStage<Long> executeUpdate(String query, Map<String, Object> params) {
        return RxOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     * 
     * @param query a normal HQL query
     * @param params {@link Parameters} of named parameters
     * @return the number of rows operated on.
     */
    public static CompletionStage<Long> executeUpdate(String query, Parameters params) {
        return RxOperations.executeUpdate(query, params.map());
    }

    /**
     * Marks the current transaction as "rollback-only", which means that it will not be
     * committed: it will be rolled back at the end of this transaction lifecycle.
     */
    public static void setRollbackOnly() {
        RxOperations.setRollbackOnly();
    }

}
