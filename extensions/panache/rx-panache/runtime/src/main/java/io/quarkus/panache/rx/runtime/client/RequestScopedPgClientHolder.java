package io.quarkus.panache.rx.runtime.client;

import java.util.concurrent.CompletionStage;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgConnection;
import io.reactiverse.axle.pgclient.PgPool;

/**
 * Bean that is used to manage request scoped PgClients
 */
@RequestScoped
public class RequestScopedPgClientHolder {

    private CompletionStage<PgConnection> con;

    public CompletionStage<? extends PgClient> getOrCreatePgClient(PgPool factory) {
        if (con != null)
            return con;
        System.err.println("PG CLIENT -> request scoped open " + this);
        con = factory.getConnection();
        return con;
    }

    @PreDestroy
    public void destroy() {
        System.err.println("PG CLIENT -> request scoped close " + this);
        if (con != null)
            con.thenAccept(con -> con.close());
    }
}
