package io.quarkus.arc.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class ArcStorageProcessor {
    @BuildStep
    public IndexDependencyBuildItem scanForStorage() {
        return new IndexDependencyBuildItem("io.quarkus.arc", "arc");
    }
}
