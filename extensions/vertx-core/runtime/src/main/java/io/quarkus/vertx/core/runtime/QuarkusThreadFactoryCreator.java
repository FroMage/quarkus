package io.quarkus.vertx.core.runtime;

import java.util.concurrent.TimeUnit;

import io.vertx.core.VertxThreadFactoryCreator;
import io.vertx.core.impl.BlockedThreadChecker;
import io.vertx.core.impl.VertxThreadFactory;

public class QuarkusThreadFactoryCreator implements VertxThreadFactoryCreator {

    @Override
    public VertxThreadFactory createVertxThreadFactory(String prefix, BlockedThreadChecker checker, boolean worker,
            long maxExecTime,
            TimeUnit maxExecTimeUnit) {
        return new QuarkusVertxThreadFactory(prefix, checker, worker, maxExecTime, maxExecTimeUnit);
    }

}
