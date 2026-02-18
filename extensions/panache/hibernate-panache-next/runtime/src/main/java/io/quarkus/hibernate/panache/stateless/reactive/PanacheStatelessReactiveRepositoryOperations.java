package io.quarkus.hibernate.panache.stateless.reactive;

import java.util.stream.Stream;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessRepositoryOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheStatelessReactiveRepositoryOperations<Entity, Id>
        extends
        PanacheStatelessRepositoryOperations<Entity, Uni<Mutiny.StatelessSession>, Uni<Entity>, Uni<Void>, Uni<Boolean>, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveStateless();
    }

    // Operations

    @Override
    default Uni<Mutiny.StatelessSession> getSession() {
        return operations().getStatelessSession(getEntityClass());
    }

    @Override
    default Uni<Entity> insert(Entity entity) {
        return operations().insert(entity).replaceWith(entity);
    }

    @Override
    default Uni<Entity> delete(Entity entity) {
        return operations().delete(entity).replaceWith(entity);
    }

    @Override
    default Uni<Entity> update(Entity entity) {
        return operations().update(entity).replaceWith(entity);
    }

    @Override
    default Uni<Entity> upsert(Entity entity) {
        return operations().upsert(entity).replaceWith(entity);
    }

    @Override
    default Uni<Void> insert(Iterable<Entity> entities) {
        return operations().insert(entities);
    }

    @Override
    default Uni<Void> insert(Stream<Entity> entities) {
        return operations().insert(entities);
    }

    @Override
    default Uni<Void> insert(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().insert(firstEntity, entities);
    }
}
