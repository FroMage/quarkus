package io.quarkus.panache.rx;

import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.runtime.RxOperations;

/**
 * <p>
 * Represents a Repository for a specific type of entity {@code Entity}, with an ID type
 * of {@code Id}. Implementing this repository will gain you the exact same useful methods
 * that are on {@link PanacheEntityBase}. Unless you have a custom ID strategy, you should not
 * implement this interface directly but implement {@link PanacheRepository} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity> The type of entity to operate on
 * @param <Id> The ID type of the entity
 */
public interface PanacheRxRepositoryBase<Entity extends PanacheRxEntityBase<Entity>, Id> {

    public default CompletionStage<? extends Entity> save(Entity entity) {
        return RxOperations.save(entity);
    }

    public default CompletionStage<Void> delete(Entity entity) {
        return RxOperations.delete(entity);
    }

    //
    // Static Helpers

    public default Publisher<Entity> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public default CompletionStage<Entity> findById(Id id) {
        throw new RuntimeException("Should never be called");
    }

    public default Publisher<Entity> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default CompletionStage<Long> count() {
        throw new RuntimeException("Should never be called");
    }

    public default CompletionStage<Long> count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default CompletionStage<Long> deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public default CompletionStage<Long> delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }
}