package io.quarkus.panache.rx;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.rx.runtime.RxOperations;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

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

    public default Single<? extends Entity> save(Entity entity) {
        return RxOperations.save(entity);
    }

    public default Completable delete(Entity entity) {
        return RxOperations.delete(entity);
    }

    //
    // Static Helpers

    public default Observable<Entity> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public default Maybe<Entity> findById(Id id) {
        throw new RuntimeException("Should never be called");
    }

    public default Observable<Entity> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> count() {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }
}