package io.quarkus.it.panache.reactive;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class Hibernate63FunctionalityTest {

    @Test
    public void testHibernate63() throws Exception {
        RestAssured.when().get("/test/hibernate63").then().body(is("OK"));
    }
}
