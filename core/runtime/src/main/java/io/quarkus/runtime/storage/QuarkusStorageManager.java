package io.quarkus.runtime.storage;

import java.util.Map;

import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

public class QuarkusStorageManager implements StorageManager {

    private Map<String, ThreadLocal<?>> declaredStorages;
    private int contextCount;

    QuarkusStorageManager(Map<String, ThreadLocal<?>> declaredStorages) {
        this.contextCount = declaredStorages.size();
        this.declaredStorages = declaredStorages;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> getThreadLocal(Class<T> klass) {
        ThreadLocal<?> storage = declaredStorages.get(klass.getName());
        if (storage != null)
            return (ThreadLocal<X>) storage;
        throw new IllegalArgumentException("Storage user nor registered: " + klass);
    }

    public static QuarkusStorageManager instance() {
        return (QuarkusStorageManager) StorageManager.instance();
    }

    public Object[] newContext() {
        return new Object[contextCount];
    }
}
