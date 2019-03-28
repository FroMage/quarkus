package io.quarkus.example.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.panache.rx.PanacheRxRepository;

@ApplicationScoped
public class RxPersonRepository implements PanacheRxRepository<RxPerson> {
}
