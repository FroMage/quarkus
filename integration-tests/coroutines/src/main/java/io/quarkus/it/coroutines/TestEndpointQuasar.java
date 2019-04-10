/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.it.coroutines;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.github.fromage.quasi.fibers.Fiber;
import com.github.fromage.quasi.fibers.FiberAsync;
import com.github.fromage.quasi.fibers.Suspendable;
import com.github.fromage.quasi.strands.SuspendableCallable;

/**
 * Various tests covering Coroutines functionality. All tests should work in both standard JVM and SubstrateVM.
 */
@Path("test-quasar")
public class TestEndpointQuasar {

    @Path("async")
    @GET
    public CompletionStage<String> getAsync() {
        return fiber(() -> {
            CompletableFuture<String> cs = new CompletableFuture<>();
            new Thread() {
                public void run() {
                    cs.complete("Stef");
                }
            }.start();
            return "Hello " + await(cs);
        });
    }

    private static <T> CompletionStage<T> fiber(SuspendableCallable<T> c) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        Fiber<Void> fiber = new Fiber<Void>(() -> {
            try {
                T val = c.run();
                ret.complete(val);
            } catch (Throwable t) {
                ret.completeExceptionally(t);
            }
        });
        fiber.start();
        return ret;
    }

    @Suspendable
    private static <T> T await(CompletionStage<T> cs) {
        try {
            return new FiberAsync<T, Throwable>() {
                @Override
                protected void requestAsync() {
                    cs.whenComplete((ret, t) -> {
                        if (t != null)
                            asyncFailed(t);
                        else
                            asyncCompleted(ret);
                    });
                }
            }.run();
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
