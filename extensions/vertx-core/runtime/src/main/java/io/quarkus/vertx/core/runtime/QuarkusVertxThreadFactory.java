package io.quarkus.vertx.core.runtime;

import java.util.concurrent.TimeUnit;

import io.vertx.core.impl.BlockedThreadChecker;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.impl.VertxThreadFactory;

public class QuarkusVertxThreadFactory extends VertxThreadFactory {

    QuarkusVertxThreadFactory(String prefix, BlockedThreadChecker checker, boolean worker, long maxExecTime,
            TimeUnit maxExecTimeUnit) {
        super(prefix, checker, worker, maxExecTime, maxExecTimeUnit);
    }

    @Override
    protected VertxThread createVertxThread(Runnable target, String name, boolean worker, long maxExecTime,
            TimeUnit maxExecTimeUnit) {
        return new QuarkusVertxThread(target, name, worker, maxExecTime, maxExecTimeUnit);
    }
}
