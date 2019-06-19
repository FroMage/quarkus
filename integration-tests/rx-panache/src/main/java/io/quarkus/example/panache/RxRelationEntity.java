package io.quarkus.example.panache;

import java.util.concurrent.CompletionStage;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import io.quarkus.panache.rx.PanacheRxEntity;

@Entity
public class RxRelationEntity extends PanacheRxEntity<RxRelationEntity> {
    @OneToOne
    public CompletionStage<RxOneToOneEntity> oneToOne;
}
