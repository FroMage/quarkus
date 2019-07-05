package io.quarkus.panache.rx;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.persistence.Transient;

import org.reactivestreams.Publisher;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.impl.GenerateBridge;
import io.quarkus.panache.rx.runtime.RxOperations;

/**
 * <p>
 * Represents an entity. If your Hibernate entities extend this class they gain
 * auto-generated accessors to all their public fields (unless annotated with
 * {@link Transient}), as well as a lot of useful methods. Unless you have a
 * custom ID strategy, you should not extend this class directly but extend
 * {@link PanacheEntity} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @see PanacheEntity
 */
public abstract class PanacheRxEntityBase<T extends PanacheRxEntityBase<T>> {

    // FIXME: should be CompletionStage<T>?
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public CompletionStage<? extends T> save() {
        return (CompletionStage) RxOperations.save(this);
    }

    public CompletionStage<Void> delete() {
        return RxOperations.delete(this);
    }

    // FIXME: hide somehow
    public RxModelInfo<T> getModelInfo() {
        throw new RuntimeException("Should never be called");
    }

    public boolean isPersistent() {
        return getModelInfo().isPersistent((T) this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;
        return Objects.equals(getModelInfo().getId((T) this), getModelInfo().getId((T) obj));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getModelInfo().getId((T) this));
    }

    //
    // Static Helpers

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<T> findById(Object id) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> find(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> find(String query, Sort sort, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> find(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> find(String query, Sort sort,
            Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> find(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> find(String query, Sort sort, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> findAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> PanacheRxQuery<T> findAll(Sort sort) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> stream(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> stream(String query, Sort sort, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> stream(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> stream(String query, Sort sort, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> stream(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> stream(String query, Sort sort, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> streamAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> Publisher<T> streamAll(Sort sort) {
        throw RxOperations.implementationInjectionMissing();
    }

    // FIXME: Java stream version?

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> list(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> list(String query, Sort sort, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> list(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> list(String query, Sort sort,
            Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> list(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> list(String query, Sort sort, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> listAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static <T extends PanacheRxEntityBase<?>> CompletionStage<List<T>> listAll(Sort sort) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> count() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> count(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> count(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> count(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> deleteAll() {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> delete(String query, Object... params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> delete(String query, Map<String, Object> params) {
        throw RxOperations.implementationInjectionMissing();
    }

    @GenerateBridge
    public static CompletionStage<Long> delete(String query, Parameters params) {
        throw RxOperations.implementationInjectionMissing();
    }

    public static CompletionStage<Void> save(Iterable<? extends PanacheRxEntityBase<?>> entities) {
        return RxOperations.save(entities);
    }

    public static CompletionStage<Void> save(Stream<? extends PanacheRxEntityBase<?>> entities) {
        return RxOperations.save(entities);
    }

    public static CompletionStage<Void> save(PanacheRxEntityBase<?> firstEntity, PanacheRxEntityBase<?>... entities) {
        return RxOperations.save(firstEntity, entities);
    }
}
