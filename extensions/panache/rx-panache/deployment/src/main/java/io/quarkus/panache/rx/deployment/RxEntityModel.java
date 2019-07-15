package io.quarkus.panache.rx.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import io.quarkus.panache.common.deployment.EntityModel;

public class RxEntityModel extends EntityModel<RxEntityField> {

    final String entityName;
    final RxMetamodelInfo modelInfo;
    private RxEntityField idField;
    String tableName;

    public RxEntityModel(ClassInfo classInfo, RxMetamodelInfo modelInfo) {
        super(classInfo);
        this.modelInfo = modelInfo;
        this.entityName = classInfo.simpleName();
        AnnotationInstance table = classInfo.classAnnotation(JpaNames.DOTNAME_TABLE);
        if (table != null) {
            AnnotationValue value = table.value("name");
            if (value != null)
                tableName = value.asString();
        }
        if (tableName == null)
            tableName = entityName;

    }

    public RxEntityField getIdField() {
        if (idField == null)
            idField = computeIdField();
        return idField;
    }

    private RxEntityField computeIdField() {
        for (RxEntityField field : fields.values()) {
            if (field.isId)
                return field;
        }
        if (superClassName != null)
            return modelInfo.getEntityModel(superClassName).getIdField();

        throw new RuntimeException("Failed to find ID field for entity " + name);
    }

}
