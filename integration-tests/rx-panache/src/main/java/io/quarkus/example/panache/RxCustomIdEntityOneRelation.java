package io.quarkus.example.panache;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.PanacheRxEntityBase;

@Entity
public class RxCustomIdEntityOneRelation extends PanacheRxEntityBase<RxCustomIdEntityOneRelation> {

    @Id
    public String myId;

    @OneToMany(mappedBy = "oneRelation")
    public Publisher<RxCustomIdEntity> entities;

}
