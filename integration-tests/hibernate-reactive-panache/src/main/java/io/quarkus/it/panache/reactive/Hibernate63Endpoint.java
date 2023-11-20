package io.quarkus.it.panache.reactive;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

@Path("test/hibernate63")
public class Hibernate63Endpoint {

    @Inject
    Hibernate63Repository repo;

    @Inject
    Hibernate63RepositoryNaked repoNaked;

    @GET
    @Path("")
    @WithTransaction
    public Uni<String> testModel() {
        Hibernate63Entity stef = new Hibernate63Entity();
        stef.field = "stef";
        Hibernate63Entity octave = new Hibernate63Entity();
        octave.field = "octave";
        return Hibernate63Entity.deleteAll()
                .chain(x -> stef.persist())
                .chain(x -> octave.persist())
                .chain(x -> Hibernate63Entity.findByField("stef"))
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return Hibernate63Entity.findByFieldHql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return Hibernate63Entity.findByFieldSql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return Hibernate63Entity.deleteField("other");
                })
                .chain(count -> {
                    Assertions.assertEquals(0, count);
                    return Hibernate63Entity.updateField("nomatch", "something");
                })
                .chain(ignore -> {
                    return repo.findByFieldHql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return repo.findByFieldHql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return repo.findByFieldSql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return repo.deleteField("other");
                })
                .chain(count -> {
                    Assertions.assertEquals(0, count);
                    return repo.updateField("nomatch", "something");
                })
                .chain(ignore -> {
                    return repoNaked.findByFieldHql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return repoNaked.findByFieldHql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return repoNaked.findByFieldSql("stef");
                })
                .chain(fromDb -> {
                    Assertions.assertEquals(stef, fromDb);
                    return repoNaked.deleteField("other");
                })
                .chain(count -> {
                    Assertions.assertEquals(0, count);
                    return repoNaked.updateField("nomatch", "something");
                })
                .map(x -> "OK");
    }

}
