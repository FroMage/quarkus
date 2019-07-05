package io.quarkus.panache.rx.deployment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.ClassInfo;

public class EntityModel {

    final String name;
    final String superClassName;
    final Map<String, EntityField> fields = new LinkedHashMap<>();
    final String tableName;
    private EntityField idField;
    private Map<String, EntityModel> entities;

    public EntityModel(ClassInfo classInfo, Map<String, EntityModel> entities) {
        this.name = classInfo.name().toString();
        this.superClassName = classInfo.superName().toString();
        this.entities = entities;
        // FIXME: read @Table annotation
        this.tableName = classInfo.simpleName();
    }

    public EntityField getIdField() {
        if(idField == null)
            idField = computeIdField();
        return idField;
    }

    private EntityField computeIdField() {
        for (EntityField field : fields.values()) {
            if(field.isId)
                return field;
        }
        if (superClassName != null)
            return entities.get(superClassName).getIdField();
        
        throw new RuntimeException("Failed to find ID field for entity " + name);
    }

    public void addField(EntityField field) {
        fields.put(field.name, field);
    }
}
