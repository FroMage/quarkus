package io.quarkus.vertx.core.runtime;

import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.storage.QuarkusStorageManager;
import io.quarkus.runtime.storage.QuarkusThread;
import io.vertx.core.impl.VertxThread;

public class QuarkusVertxThread extends VertxThread implements QuarkusThread {

    private final Object[] contexts = QuarkusStorageManager.instance().newContext();

    public QuarkusVertxThread(Runnable target, String name, boolean worker, long maxExecTime, TimeUnit maxExecTimeUnit) {
        super(target, name, worker, maxExecTime, maxExecTimeUnit);
    }

    @Override
    public Object[] getQuarkusThreadContext() {
        return contexts;
    }

}
