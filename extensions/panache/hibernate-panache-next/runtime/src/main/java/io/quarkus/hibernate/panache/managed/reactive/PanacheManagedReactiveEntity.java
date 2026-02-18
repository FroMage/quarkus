package io.quarkus.hibernate.panache.managed.reactive;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.managed.PanacheManagedEntityOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheManagedReactiveEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheManagedEntityOperations<Entity, Uni<Entity>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    @Override
    public default Uni<Entity> persist() {
        return operations().persist(this).replaceWith((Entity) this);
    }

    @Override
    public default Uni<Entity> persistAndFlush() {
        return operations().persistAndFlush(this).replaceWith((Entity) this);
    }

    @Override
    public default Uni<Entity> delete() {
        return operations().delete(this).replaceWith((Entity) this);
    }

    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    @Override
    public default Uni<Boolean> isPersistent() {
        return operations().isPersistent(this);
    }

}
