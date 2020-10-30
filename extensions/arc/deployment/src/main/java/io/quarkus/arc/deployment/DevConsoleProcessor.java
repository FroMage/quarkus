package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.DevConsoleProvider;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class DevConsoleProcessor {
    @BuildStep
    public AdditionalBeanBuildItem registerArcContainer(LaunchModeBuildItem launchMode) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            return AdditionalBeanBuildItem.unremovableOf(DevConsoleProvider.class);
        }
        return null;
    }
}
