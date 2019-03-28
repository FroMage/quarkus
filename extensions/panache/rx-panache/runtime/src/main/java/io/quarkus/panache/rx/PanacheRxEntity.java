package io.quarkus.panache.rx;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * <p>
 * Represents an entity with a generated ID field {@link #id} of type {@link Long}. If your
 * Hibernate entities extend this class they gain the ID field and auto-generated accessors
 * to all their public fields (unless annotated with {@link Transient}), as well as all
 * the useful methods from {@link PanacheEntityBase}.
 * </p>
 * <p>
 * If you want a custom ID type or strategy, you can directly extend {@link PanacheEntityBase}
 * instead, and write your own ID field. You will still get auto-generated accessors and
 * all the useful methods.
 * </p>
 *
 * @author Stéphane Épardaud
 * @see PanacheEntityBase
 */
@MappedSuperclass
public abstract class PanacheRxEntity<T extends PanacheRxEntity<T>> extends PanacheRxEntityBase<T> {

    @Id
    public Long id;

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    // FIXME: use regular accessors?
    @Override
    public Object _getId() {
        return id;
    }

    @Override
    public void _setId(Object id) {
        this.id = (Long) id;
    }
}