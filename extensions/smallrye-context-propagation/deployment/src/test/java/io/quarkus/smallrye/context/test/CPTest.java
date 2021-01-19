package io.quarkus.smallrye.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.storage.QuarkusThread;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.context.SmallRyeThreadContext;
import io.vertx.core.Vertx;

public class CPTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.vertx.storage", "false")
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses();
                }
            });

    @Inject
    SmallRyeThreadContext tc;

    @Inject
    Vertx vertx;

    @Test
    public void test() throws InterruptedException, ExecutionException {
        System.err.println("Current thread: " + Thread.currentThread().getClass());
        System.err.println("Plan: " + tc.getPlan().propagatedProviders);
        Arc.container().requestContext().activate();
        System.err.println("Request context: " + Arc.container().requestContext().isActive());

        Runnable runner = tc.contextualRunnable(() -> {
            System.err.println("Runner thread supports storage: " + (Thread.currentThread() instanceof QuarkusThread));
            System.err.println("Runner request context: " + Arc.container().requestContext().isActive());
            System.err.println("Runner called with context");
        });

        CompletableFuture<Object> cf = new CompletableFuture<>();
        vertx.executeBlocking(prom -> {
            runner.run();
            prom.complete();
        }, res -> {
            if (res.succeeded())
                cf.complete(res.result());
            else
                cf.completeExceptionally(res.cause());
            System.err.println("Got result: " + res);
        });
        cf.get();
        System.err.println("Done");
        Arc.container().requestContext().destroy();
    }
}
