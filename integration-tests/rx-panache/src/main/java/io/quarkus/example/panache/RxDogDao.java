package io.quarkus.example.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.panache.rx.PanacheRxRepositoryBase;

// custom id type
@ApplicationScoped
public class RxDogDao implements PanacheRxRepositoryBase<RxDog, Integer> {

}
