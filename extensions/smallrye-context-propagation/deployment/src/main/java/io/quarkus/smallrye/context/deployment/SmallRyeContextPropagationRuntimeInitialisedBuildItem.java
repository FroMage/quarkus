package io.quarkus.smallrye.context.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item to be able to run after SR-CP has had time to configure itself at runtime.
 */
public final class SmallRyeContextPropagationRuntimeInitialisedBuildItem extends SimpleBuildItem {

}
