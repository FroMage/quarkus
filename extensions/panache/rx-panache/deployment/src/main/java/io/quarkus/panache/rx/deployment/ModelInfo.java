package io.quarkus.panache.rx.deployment;

import java.util.HashMap;
import java.util.Map;

public class ModelInfo {
    final Map<String, EntityModel> entities = new HashMap<>();
    final Map<String, String> sequenceGenerators = new HashMap<>();

    public EntityModel getEntityModel(String className) {
        return entities.get(className);
    }

    public void addEntityModel(EntityModel entityModel) {
        entities.put(entityModel.name, entityModel);
    }

    public boolean hasEntities() {
        return !entities.isEmpty();
    }

    public void addSequenceGenerator(String name, String sequenceName) {
        if(sequenceGenerators.containsKey(name))
            throw new IllegalArgumentException("Sequence generator "+name+" already declared");
        sequenceGenerators.put(name, sequenceName);
    }

    public String getSequenceName(String name) {
        String ret = sequenceGenerators.get(name);
        return ret != null ? ret : name;
    }
}
