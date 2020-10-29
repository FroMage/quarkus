package io.quarkus.rest.server.runtime.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ScoreSystemProducer {
    static volatile ScoreSystem.EndpointScores endpoints;

    @Named("quarkusRestEndpointScores")
    @Produces
    @Singleton
    public ScoreSystem.EndpointScores getEndpointScores() {
        return endpoints;
    }

}
