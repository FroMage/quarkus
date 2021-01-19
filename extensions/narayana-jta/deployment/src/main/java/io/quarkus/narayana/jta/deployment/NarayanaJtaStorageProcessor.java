package io.quarkus.narayana.jta.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class NarayanaJtaStorageProcessor {
    @BuildStep
    public IndexDependencyBuildItem scanForStorage() {
        return new IndexDependencyBuildItem("org.jboss.narayana.jta", "narayana-jta");
    }
}
