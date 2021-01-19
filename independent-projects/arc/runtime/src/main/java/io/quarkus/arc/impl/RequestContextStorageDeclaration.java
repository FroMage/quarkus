package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.smallrye.context.storage.spi.StorageDeclaration;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.spi.Contextual;

public class RequestContextStorageDeclaration
        implements StorageDeclaration<ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>>> {

}
