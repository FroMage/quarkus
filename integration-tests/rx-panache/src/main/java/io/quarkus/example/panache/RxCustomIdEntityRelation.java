package io.quarkus.example.panache;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.PanacheRxEntityBase;

@Entity
public class RxCustomIdEntityRelation extends PanacheRxEntityBase<RxCustomIdEntityRelation> {

    @Id
    public String myId;

    @ManyToMany(mappedBy = "relations")
    public Publisher<RxCustomIdEntity> entities;

}
