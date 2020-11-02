package io.quarkus.rest.server.runtime.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.common.runtime.util.ServerMediaType;
import io.quarkus.rest.server.runtime.mapping.RuntimeResource;

public class ScoreSystem {

    public static class EndpointScores {

        public final List<EndpointScore> endpoints;
        public final int score;

        public EndpointScores(int score, List<EndpointScore> endpoints) {
            this.score = score;
            this.endpoints = endpoints;
        }

    }

    public static class EndpointScore {

        public final String httpMethod;
        public final String fullPath;
        public final List<MediaType> produces;
        public final List<MediaType> consumes;
        public final Map<Category, List<Diagnostic>> diagnostics;
        public final int score;

        public EndpointScore(String httpMethod, String fullPath, List<MediaType> produces, List<MediaType> consumes,
                Map<Category, List<Diagnostic>> diagnostics, int score) {
            this.httpMethod = httpMethod;
            this.fullPath = fullPath;
            this.produces = produces;
            this.consumes = consumes;
            this.diagnostics = diagnostics;
            this.score = score;
        }

    }

    public static class Diagnostic {

        public final String message;
        public final int score;

        public Diagnostic(String message, int score) {
            this.message = message;
            this.score = score;
        }

        @Override
        public String toString() {
            return message + ": " + score + "/100";
        }

        public static Diagnostic ExecutionNonBlocking = new Diagnostic("Dispatched on the IO thread", 100);
        public static Diagnostic ExecutionBlocking = new Diagnostic("Needs a worker thread dispatch", 0);

        public static Diagnostic ResourceSingleton = new Diagnostic("Single resource instance for all requests", 100);
        public static Diagnostic ResourcePerRequest = new Diagnostic("New resource instance for every request", 0);

        public static Diagnostic WriterBuildTime(MessageBodyWriter<?> buildTimeWriter) {
            return new Diagnostic("Single writer set at build time: " + buildTimeWriter, 90);
        }

        public static Diagnostic WriterBuildTimeDirect(MessageBodyWriter<?> buildTimeWriter) {
            return new Diagnostic("Single direct writer set at build time: " + buildTimeWriter, 100);
        }

        public static Diagnostic WriterBuildTimeMultiple(List<MessageBodyWriter<?>> buildTimeWriters) {
            return new Diagnostic("Multiple writers set at build time: " + buildTimeWriters, 50);
        }

        public static Diagnostic WriterRunTime = new Diagnostic("Run time writers required", 0);
        public static Diagnostic WriterNotRequired = new Diagnostic("No writers required", 100);

    }

    public enum Category {
        Writer,
        Resource,
        Execution
    }

    public final static RuntimeResourceVisitor ScoreVisitor = new RuntimeResourceVisitor() {
        int overallScore = 0;
        int overallTotal = 0;
        List<EndpointScore> endpoints = new ArrayList<>();

        @Override
        public void visitRuntimeResource(String httpMethod, String fullPath, RuntimeResource runtimeResource) {
            ServerMediaType serverMediaType = runtimeResource.getProduces();
            List<MediaType> produces = Collections.emptyList();
            if (serverMediaType != null) {
                if ((serverMediaType.getSortedOriginalMediaTypes() != null)
                        && serverMediaType.getSortedOriginalMediaTypes().length >= 1) {
                    produces = Arrays.asList(serverMediaType.getSortedOriginalMediaTypes());
                }
            }
            List<MediaType> consumes = runtimeResource.getConsumes();
            int score = 0;
            int total = 0;
            for (Entry<Category, List<Diagnostic>> scoreEntry : runtimeResource.getScore().entrySet()) {
                for (Diagnostic diagnostic : scoreEntry.getValue()) {
                    score += diagnostic.score;
                }
                total += 100;
            }
            // let's bring it to 100
            score = (int) Math.floor(((float) score / (float) total) * 100f);
            overallScore += score;
            overallTotal += 100;
            endpoints.add(new EndpointScore(httpMethod, fullPath, produces, consumes, runtimeResource.getScore(), score));
        }

        @Override
        public void visitEnd() {
            // let's bring it to 100
            overallScore = (int) Math.floor(((float) overallScore / (float) overallTotal) * 100f);
            ScoreSystemProducer.endpoints = new EndpointScores(overallScore, endpoints);
        }
    };
}
