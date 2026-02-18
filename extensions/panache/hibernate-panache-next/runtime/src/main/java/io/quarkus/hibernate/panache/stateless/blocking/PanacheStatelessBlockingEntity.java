package io.quarkus.hibernate.panache.stateless.blocking;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;

public interface PanacheStatelessBlockingEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheStatelessEntityOperations<Entity, Entity, Boolean> {

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingStateless();
    }

    @Override
    public default Entity insert() {
        operations().insert(this);
        return (Entity) this;
    }

    @Override
    public default Entity delete() {
        operations().delete(this);
        return (Entity) this;
    }

    @Override
    public default Entity update() {
        operations().update(this);
        return (Entity) this;
    }

    @Override
    public default Entity upsert() {
        operations().upsert(this);
        return (Entity) this;
    }
}
