package io.quarkus.panache.rx.runtime.client;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgPool;

@ApplicationScoped
public class TransactionPgClients {

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Inject
    TransactionManager transactionManager;

    @Inject
    PgPool pgPool;

    @Inject
    Instance<RequestScopedPgClientHolder> requestScopedEntityManagers;

    @Inject
    ThreadContext threadContext;

    @Singleton
    @Produces
    public PgClient getPgClient() {
        return new TransactionScopedPgClient(transactionManager, transactionSynchronizationRegistry,
                pgPool, threadContext,
                requestScopedEntityManagers);
    }

}
