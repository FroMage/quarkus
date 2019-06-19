package io.quarkus.example.panache;

import java.util.concurrent.CompletionStage;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import io.quarkus.panache.rx.PanacheRxEntity;

@Entity
public class RxOneToOneEntity extends PanacheRxEntity<RxOneToOneEntity> {
    @OneToOne(mappedBy = "oneToOne")
    public CompletionStage<RxRelationEntity> relationEntity;
}
