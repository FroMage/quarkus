package io.quarkus.panache.rx.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class PgPoolRuntimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DataSourceRuntimeConfig defaultDataSource;

    //    /**
    //     * Additional datasources.
    //     */
    //    @ConfigItem(name = ConfigItem.PARENT)
    //    public Map<String, DataSourceRuntimeConfig> namedDataSources;
}
