package io.quarkus.hibernate.panache.managed.blocking;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.managed.PanacheManagedEntityOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;

public interface PanacheManagedBlockingEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheManagedEntityOperations<Entity, Entity, Boolean> {

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingManaged();
    }

    @Override
    public default Entity persist() {
        operations().persist(this);
        return (Entity) this;
    }

    @Override
    public default Entity persistAndFlush() {
        operations().persistAndFlush(this);
        return (Entity) this;
    }

    @Override
    public default Entity delete() {
        operations().delete(this);
        return (Entity) this;
    }

    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    @Override
    public default Boolean isPersistent() {
        return operations().isPersistent(this);
    }

}
