package io.quarkus.panache.rx.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ModelClassesBuildItem extends SimpleBuildItem {

    public final Set<String> modelClasses;

    public ModelClassesBuildItem(Set<String> modelClasses) {
        this.modelClasses = modelClasses;
    }

}
