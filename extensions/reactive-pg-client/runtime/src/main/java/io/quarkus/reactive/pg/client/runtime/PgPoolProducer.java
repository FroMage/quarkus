package io.quarkus.reactive.pg.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Singleton;

import io.reactiverse.pgclient.PgPool;

@ApplicationScoped
public class PgPoolProducer {

    private volatile PgPool pgPool;
    private volatile io.reactiverse.axle.pgclient.PgPool axlePgPool;
    private volatile io.reactiverse.reactivex.pgclient.PgPool rxPgPool;

    void initialize(PgPool pgPool) {
        this.pgPool = pgPool;
        this.axlePgPool = io.reactiverse.axle.pgclient.PgPool.newInstance(pgPool);
        this.rxPgPool = io.reactiverse.reactivex.pgclient.PgPool.newInstance(pgPool);
    }

    @Typed(value = PgPool.class)
    @Singleton
    @Produces
    public PgPool pgPool() {
        return pgPool;
    }

    @Typed(value = io.reactiverse.axle.pgclient.PgPool.class)
    @Singleton
    @Produces
    public io.reactiverse.axle.pgclient.PgPool axlePgPool() {
        return axlePgPool;
    }

    @Typed(value = io.reactiverse.reactivex.pgclient.PgPool.class)
    @Singleton
    @Produces
    public io.reactiverse.reactivex.pgclient.PgPool rxPgPool() {
        return rxPgPool;
    }
}
