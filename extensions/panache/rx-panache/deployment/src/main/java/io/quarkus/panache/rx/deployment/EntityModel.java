package io.quarkus.panache.rx.deployment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

public class EntityModel {

    final String name;
    final String superClassName;
    final Map<String, EntityField> fields = new LinkedHashMap<>();
    final String entityName;
    final ModelInfo modelInfo;
    private EntityField idField;
    String tableName;

    public EntityModel(ClassInfo classInfo, ModelInfo modelInfo) {
        this.name = classInfo.name().toString();
        this.superClassName = classInfo.superName().toString();
        this.modelInfo = modelInfo;
        this.entityName = classInfo.simpleName();
        AnnotationInstance table = classInfo.classAnnotation(JpaNames.DOTNAME_TABLE);
        if(table != null) {
            AnnotationValue value = table.value("name");
            if(value != null)
                tableName = value.asString();
        }
        if(tableName == null)
            tableName = entityName;

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
            return modelInfo.getEntityModel(superClassName).getIdField();
        
        throw new RuntimeException("Failed to find ID field for entity " + name);
    }

    public void addField(EntityField field) {
        fields.put(field.name, field);
    }
}
