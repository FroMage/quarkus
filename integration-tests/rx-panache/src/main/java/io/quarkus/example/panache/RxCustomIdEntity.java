package io.quarkus.example.panache;

import java.util.concurrent.CompletionStage;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.PanacheRxEntityBase;

@Entity
public class RxCustomIdEntity extends PanacheRxEntityBase<RxCustomIdEntity> {

    @Id
    public String myId;
    
    @ManyToMany
    public Publisher<RxCustomIdEntityRelation> relations;

    @ManyToOne
    public CompletionStage<RxCustomIdEntityOneRelation> oneRelation;
}
