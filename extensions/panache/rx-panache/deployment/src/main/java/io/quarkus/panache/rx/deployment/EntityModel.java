package io.quarkus.panache.rx.deployment;

import java.util.Map;

import org.jboss.jandex.ClassInfo;

public class EntityModel {

    final String name;
    final String superClassName;
    final Map<String, EntityField> fields;
    public EntityField idField;

    public EntityModel(ClassInfo classInfo, Map<String, EntityField> fields, EntityField idField) {
        this.name = classInfo.name().toString();
        this.superClassName = classInfo.superName().toString();
        this.fields = fields;
        this.idField = idField;
    }

}
