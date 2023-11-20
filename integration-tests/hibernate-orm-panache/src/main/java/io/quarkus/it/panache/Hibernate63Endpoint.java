package io.quarkus.it.panache;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

@Path("test/hibernate63")
public class Hibernate63Endpoint {

    @Inject
    Hibernate63Repository repo;

    @Inject
    Hibernate63RepositoryNaked repoNaked;

    @GET
    @Path("")
    @Transactional
    public String testModel() {
        Hibernate63Entity.deleteAll();

        Hibernate63Entity stef = new Hibernate63Entity();
        stef.field = "stef";
        stef.persist();

        Hibernate63Entity octave = new Hibernate63Entity();
        octave.field = "octave";
        octave.persist();

        Assertions.assertEquals(stef, Hibernate63Entity.findByField("stef"));
        Assertions.assertEquals(stef, Hibernate63Entity.findByFieldHql("stef"));
        Assertions.assertEquals(stef, Hibernate63Entity.findByFieldSql("stef"));
        Assertions.assertEquals(0, Hibernate63Entity.deleteField("other"));
        Hibernate63Entity.updateField("nomatch", "something");
        Assertions.assertEquals(stef, repo.findByField("stef"));
        Assertions.assertEquals(stef, repo.findByFieldHql("stef"));
        Assertions.assertEquals(stef, repo.findByFieldSql("stef"));
        Assertions.assertEquals(0, repo.deleteField("other"));
        repo.updateField("nomatch", "something");
        Assertions.assertEquals(stef, repoNaked.findByField("stef"));
        Assertions.assertEquals(stef, repoNaked.findByFieldHql("stef"));
        Assertions.assertEquals(stef, repoNaked.findByFieldSql("stef"));
        Assertions.assertEquals(0, repoNaked.deleteField("other"));
        repoNaked.updateField("nomatch", "something");
        return "OK";
    }

}
