package io.quarkus.example.panache;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import org.reactivestreams.Publisher;

import io.quarkus.panache.rx.PanacheRxEntity;

@Entity
public class RxManyToManyEntity extends PanacheRxEntity<RxManyToManyEntity> {
    @ManyToMany(mappedBy = "manyToManys")
    public Publisher<RxRelationEntity> relations;
}
