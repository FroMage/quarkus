package io.quarkus.panache.rx.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.panache.common.deployment.MetamodelInfo;

public class RxMetamodelInfo extends MetamodelInfo<RxEntityModel> {
    final Map<String, String> sequenceGenerators = new HashMap<>();

    public void addSequenceGenerator(String name, String sequenceName) {
        if (sequenceGenerators.containsKey(name))
            throw new IllegalArgumentException("Sequence generator " + name + " already declared");
        sequenceGenerators.put(name, sequenceName);
    }

    public String getSequenceName(String name) {
        String ret = sequenceGenerators.get(name);
        return ret != null ? ret : name;
    }
}
