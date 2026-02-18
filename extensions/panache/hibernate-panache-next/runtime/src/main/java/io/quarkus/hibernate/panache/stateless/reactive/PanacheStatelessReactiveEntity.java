package io.quarkus.hibernate.panache.stateless.reactive;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheStatelessReactiveEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheStatelessEntityOperations<Entity, Uni<Entity>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    @Override
    public default Uni<Entity> insert() {
        return operations().insert(this).replaceWith((Entity) this);
    }

    @Override
    public default Uni<Entity> delete() {
        return operations().delete(this).replaceWith((Entity) this);
    }

    @Override
    public default Uni<Entity> update() {
        return operations().update(this).replaceWith((Entity) this);
    }

    @Override
    public default Uni<Entity> upsert() {
        return operations().upsert(this).replaceWith((Entity) this);
    }
}
