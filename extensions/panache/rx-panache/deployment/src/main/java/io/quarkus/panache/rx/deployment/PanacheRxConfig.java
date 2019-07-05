package io.quarkus.panache.rx.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class PanacheRxConfig {
    /**
     * To populate the database tables with data before the application loads,
     * specify the location of a load script.
     * The location specified in this property is relative to the root of the application.
     */
    @ConfigItem
    public Optional<String> sqlLoadScript;

    /**
     * Control how schema generation is happening in Panache RX.
     * <p>
     * Values: none (default on prod), create, drop-and-create (default on test), drop, update (default on dev)
     */
    @ConfigItem
    public Optional<String> generation;
}
