package io.quarkus.resteasy.common.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class ResteasyStorageProcessor {
    @BuildStep
    public IndexDependencyBuildItem scanForStorage() {
        return new IndexDependencyBuildItem("org.jboss.resteasy", "resteasy-core");
    }
}
