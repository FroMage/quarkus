package io.quarkus.runtime.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.context.storage.spi.StorageManagerProvider;

@Recorder
public class StorageRecorder {

    public void configureStaticInit(Map<String, RuntimeValue<ThreadLocal<?>>> storageMappings) {
        System.err.println("Configuring storages for: " + storageMappings);
        Map<String, ThreadLocal<?>> declaredStorages = new HashMap<>();
        for (Entry<String, RuntimeValue<ThreadLocal<?>>> entry : storageMappings.entrySet()) {
            declaredStorages.put(entry.getKey(), entry.getValue().getValue());
        }
        QuarkusStorageManagerProvider provider = new QuarkusStorageManagerProvider(declaredStorages);
        StorageManagerProvider.register(provider);
    }

}
