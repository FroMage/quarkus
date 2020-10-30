package io.quarkus.arc.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;

import io.quarkus.arc.impl.ArcContainerImpl;

/**
 * Conditionally registered in DEV mode for the DEV console
 */
@Singleton
public class DevConsoleProvider {
    @Named("arcContainer")
    @ApplicationScoped
    public ArcContainerImpl getArcContainer() {
        return ArcContainerImpl.instance();
    }
}
