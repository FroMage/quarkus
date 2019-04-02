package io.quarkus.example.panache;

import java.util.concurrent.CompletionStage;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.reactivex.Single;

// custom id type
@Entity
public class RxDog extends PanacheRxEntityBase<RxDog> {

    @Id
    @GeneratedValue
    public Integer id;

    public String name;

    public String race;

    @ManyToOne
    public CompletionStage<RxPerson> owner;

    public RxDog(String name, String race) {
        this.name = name;
        this.race = race;
    }

    @Override
    public Object _getId() {
        return id;
    }

    @Override
    public void _setId(Object id) {
        // FIXME:
        if (id instanceof Long)
            this.id = ((Long) id).intValue();
        else
            this.id = (Integer) id;
    }
}
