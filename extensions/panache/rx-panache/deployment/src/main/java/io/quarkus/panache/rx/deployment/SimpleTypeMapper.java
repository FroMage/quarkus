package io.quarkus.panache.rx.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

public class SimpleTypeMapper {

    private final DotName forType;
    private final String fromRowMethod;
    private final String toTupleMethod;

    public SimpleTypeMapper(DotName forType, String fromRowMethod, String toTupleMethod) {
        this.forType = forType;
        this.fromRowMethod = fromRowMethod;
        this.toTupleMethod = toTupleMethod;
    }

    public DotName getEntityFieldTypeName() {
        return forType;
    }

    // to be overridden
    public SimpleTypeMapper getMostPreciseMapper(FieldInfo fieldInfo) {
        return this;
    }

    public String getFromRowMethod() {
        return fromRowMethod;
    }

    public String getToTupleStoreMethod() {
        return toTupleMethod;
    }

}
