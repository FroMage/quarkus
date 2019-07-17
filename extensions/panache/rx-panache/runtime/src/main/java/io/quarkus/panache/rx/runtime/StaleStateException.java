package io.quarkus.panache.rx.runtime;

import javax.persistence.PersistenceException;

@SuppressWarnings("serial")
public class StaleStateException extends PersistenceException {

    public StaleStateException(String message) {
        super(message);
    }

}
