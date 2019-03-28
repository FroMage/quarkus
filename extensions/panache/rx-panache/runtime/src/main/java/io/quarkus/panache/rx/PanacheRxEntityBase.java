package io.quarkus.panache.rx;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.persistence.Transient;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.rx.runtime.RxOperations;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

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
    public Single<? extends T> save() {
        return (Single) RxOperations.save(this);
    }

    public Completable delete() {
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

    public static <T extends PanacheRxEntityBase<?>> Observable<T> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends PanacheRxEntityBase<?>> Maybe<T> findById(Object id) {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends PanacheRxEntityBase<?>> Observable<T> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> count() {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }
}
