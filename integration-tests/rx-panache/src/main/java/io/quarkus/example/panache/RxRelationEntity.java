package io.quarkus.example.panache;

import java.util.concurrent.CompletionStage;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.PanacheRxEntity;

@Entity
public class RxRelationEntity extends PanacheRxEntity<RxRelationEntity> {
    @OneToOne
    public CompletionStage<RxOneToOneEntity> oneToOne;

    @ManyToMany
    public Publisher<RxManyToManyEntity> manyToManys;
}
