package io.quarkus.runtime.storage;

import java.util.Map;

import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.StorageManagerProvider;

public class QuarkusStorageManagerProvider implements StorageManagerProvider {

    private final QuarkusStorageManager storageManager;

    public QuarkusStorageManagerProvider(Map<String, ThreadLocal<?>> declaredStorages) {
        storageManager = new QuarkusStorageManager(declaredStorages);
    }

    @Override
    public StorageManager getStorageManager(ClassLoader classloader) {
        return storageManager;
    }

    public QuarkusStorageManagerProvider instance() {
        return (QuarkusStorageManagerProvider) StorageManagerProvider.instance();
    }
}
