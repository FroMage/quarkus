package io.quarkus.panache.rx.runtime;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class LazyPublisher<T> implements Publisher<T> {

    private Callable<Publisher<T>> publisherSource;
    private Publisher<T> publisher;

    public LazyPublisher(Callable<Publisher<T>> publisherSource) {
        this.publisherSource = publisherSource;
    }

    private void init() {
        if (publisherSource != null) {
            synchronized (this) {
                if (publisherSource != null) {
                    try {
                        publisher = publisherSource.call();
                    } catch (Exception e) {
                        publisher = ReactiveStreams.<T> failed(e).buildRs();
                    }
                    publisherSource = null;
                }
            }
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        init();
        publisher.subscribe(s);
    }

}
