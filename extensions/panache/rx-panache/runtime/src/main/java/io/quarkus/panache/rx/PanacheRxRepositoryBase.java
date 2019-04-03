package io.quarkus.panache.rx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.impl.GenerateBridge;
import io.quarkus.panache.rx.runtime.RxOperations;

/**
 * <p>
 * Represents a Repository for a specific type of entity {@code Entity}, with an
 * ID type of {@code Id}. Implementing this repository will gain you the exact
 * same useful methods that are on {@link PanacheEntityBase}. Unless you have a
 * custom ID strategy, you should not implement this interface directly but
 * implement {@link PanacheRepository} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity>
 *            The type of entity to operate on
 * @param <Id>
 *            The ID type of the entity
 */
public interface PanacheRxRepositoryBase<Entity extends PanacheRxEntityBase<Entity>, Id> {

    public default CompletionStage<? extends Entity> save(Entity entity) {
        return RxOperations.save(entity);
    }

    public default CompletionStage<Void> delete(Entity entity) {
        return RxOperations.delete(entity);
    }

    public default boolean isPersistent(Entity entity) {
        return RxOperations.isPersistent(entity);
    }

    //
    // Static Helpers

    @GenerateBridge
    public default CompletionStage<Entity> findById(Id id) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> find(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> find(String query, Sort sort, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> find(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> find(String query, Sort sort, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> find(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> find(String query, Sort sort, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> findAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default PanacheRxQuery<Entity> findAll(Sort sort) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> list(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> list(String query, Sort sort, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> list(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> list(String query, Sort sort, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> list(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> list(String query, Sort sort, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> listAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<List<Entity>> listAll(Sort sort) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> stream(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> stream(String query, Sort sort, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> stream(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> stream(String query, Sort sort, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> stream(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> stream(String query, Sort sort, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> streamAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default Publisher<Entity> streamAll(Sort sort) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> count() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> count(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> count(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> count(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> deleteAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> delete(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> delete(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Long> delete(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Void> save(Iterable<Entity> entities) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Void> save(Stream<Entity> entities) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public default CompletionStage<Void> save(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        throw RxOperations.implementationInjectionMissing();
    }
}
