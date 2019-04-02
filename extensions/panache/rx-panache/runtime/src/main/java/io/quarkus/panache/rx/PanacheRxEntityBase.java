package io.quarkus.panache.rx;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import javax.persistence.Transient;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.runtime.RxOperations;

/**
 * <p>
 * Represents an entity. If your Hibernate entities extend this class they gain auto-generated accessors
 * to all their public fields (unless annotated with {@link Transient}), as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * {@link PanacheEntity} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @see PanacheEntity
 */
public abstract class PanacheRxEntityBase<T extends PanacheRxEntityBase<?>> {

    public abstract Object _getId();

    public abstract void _setId(Object id);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public CompletionStage<? extends T> save() {
        return (CompletionStage) RxOperations.save(this);
    }

    public CompletionStage<Void> delete() {
        return RxOperations.delete(this);
    }

    public RxModelInfo<T> getModelInfo() {
        throw new RuntimeException("Should never be called");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;
        return Objects.equals(_getId(), ((PanacheRxEntityBase) obj)._getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(_getId());
    }

    //
    // Static Helpers

    public static <T extends PanacheRxEntityBase<?>> Publisher<T> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends PanacheRxEntityBase<?>> CompletionStage<T> findById(Object id) {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends PanacheRxEntityBase<?>> Publisher<T> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static CompletionStage<Long> count() {
        throw new RuntimeException("Should never be called");
    }

    public static CompletionStage<Long> count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static CompletionStage<Long> deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public static CompletionStage<Long> delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }
}
