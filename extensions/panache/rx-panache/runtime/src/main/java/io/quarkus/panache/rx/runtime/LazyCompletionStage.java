package io.quarkus.panache.rx.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class LazyCompletionStage<T> implements CompletionStage<T> {

    private Callable<CompletionStage<T>> csSource;
    private CompletionStage<T> cs;

    public LazyCompletionStage(Callable<CompletionStage<T>> csSource) {
        this.csSource = csSource;
    }

    private void init() {
        if (csSource != null) {
            synchronized (this) {
                if (csSource != null) {
                    try {
                        cs = csSource.call();
                    } catch (Exception e) {
                        cs = RxOperations.failedFuture(e);
                    }
                    csSource = null;
                }
            }
        }
    }

    public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
        init();
        return cs.thenApply(fn);
    }

    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        init();
        return cs.thenApplyAsync(fn);
    }

    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        init();
        return cs.thenApplyAsync(fn, executor);
    }

    public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
        init();
        return cs.thenAccept(action);
    }

    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
        init();
        return cs.thenAcceptAsync(action);
    }

    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        init();
        return cs.thenAcceptAsync(action, executor);
    }

    public CompletionStage<Void> thenRun(Runnable action) {
        init();
        return cs.thenRun(action);
    }

    public CompletionStage<Void> thenRunAsync(Runnable action) {
        init();
        return cs.thenRunAsync(action);
    }

    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        init();
        return cs.thenRunAsync(action, executor);
    }

    public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        init();
        return cs.thenCombine(other, fn);
    }

    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        init();
        return cs.thenCombineAsync(other, fn);
    }

    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn,
            Executor executor) {
        init();
        return cs.thenCombineAsync(other, fn, executor);
    }

    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        init();
        return cs.thenAcceptBoth(other, action);
    }

    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        init();
        return cs.thenAcceptBothAsync(other, action);
    }

    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action,
            Executor executor) {
        init();
        return cs.thenAcceptBothAsync(other, action, executor);
    }

    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        init();
        return cs.runAfterBoth(other, action);
    }

    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        init();
        return cs.runAfterBothAsync(other, action);
    }

    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        init();
        return cs.runAfterBothAsync(other, action, executor);
    }

    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        init();
        return cs.applyToEither(other, fn);
    }

    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        init();
        return cs.applyToEitherAsync(other, fn);
    }

    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        init();
        return cs.applyToEitherAsync(other, fn, executor);
    }

    public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        init();
        return cs.acceptEither(other, action);
    }

    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        init();
        return cs.acceptEitherAsync(other, action);
    }

    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        init();
        return cs.acceptEitherAsync(other, action, executor);
    }

    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        init();
        return cs.runAfterEither(other, action);
    }

    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        init();
        return cs.runAfterEitherAsync(other, action);
    }

    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        init();
        return cs.runAfterEitherAsync(other, action, executor);
    }

    public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        init();
        return cs.thenCompose(fn);
    }

    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        init();
        return cs.thenComposeAsync(fn);
    }

    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        init();
        return cs.thenComposeAsync(fn, executor);
    }

    public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
        init();
        return cs.exceptionally(fn);
    }

    public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        init();
        return cs.whenComplete(action);
    }

    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        init();
        return cs.whenCompleteAsync(action);
    }

    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        init();
        return cs.whenCompleteAsync(action, executor);
    }

    public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        init();
        return cs.handle(fn);
    }

    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        init();
        return cs.handleAsync(fn);
    }

    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        init();
        return cs.handleAsync(fn, executor);
    }

    public CompletableFuture<T> toCompletableFuture() {
        init();
        return cs.toCompletableFuture();
    }

}
