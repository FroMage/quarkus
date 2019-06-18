package io.quarkus.panache.rx.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.reactivestreams.Publisher;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rx.PanacheRxEntity;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.quarkus.panache.rx.deployment.PanacheRxResourceProcessor.ProcessorClassOutput;
import io.quarkus.panache.rx.runtime.RxDataTypes;
import io.quarkus.panache.rx.runtime.RxOperations;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

public class PanacheRxModelInfoGenerator {

    public static void generateModelClass(String modelClassName, Map<String, EntityModel> entities,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        List<EntityField> fields = new ArrayList<>();
        collectFields(entities, modelClassName, fields);

        String modelType = modelClassName.replace('.', '/');
        String modelSignature = "L" + modelType + ";";
        // FIXME
        String tableName;
        int lastDot = modelClassName.lastIndexOf('.');
        if (lastDot != -1)
            tableName = modelClassName.substring(lastDot + 1);
        else
            tableName = modelClassName;

        String modelInfoClassName = modelClassName + PanacheRxEntityEnhancer.RX_MODEL_SUFFIX;

        ClassCreator modelClass = ClassCreator.builder().className(modelInfoClassName)
                .classOutput(new ProcessorClassOutput(generatedClasses))
                .interfaces(RxModelInfo.class)
                .signature(
                        "Ljava/lang/Object;L" + PanacheRxEntityEnhancer.RX_MODEL_INFO_BINARY_NAME + "<" + modelSignature + ">;")
                .build();

        // no arg constructor is auto-created by gizmo

        // instance field
        FieldCreator instanceField = modelClass.getFieldCreator(PanacheRxEntityEnhancer.RX_MODEL_FIELD_NAME,
                modelInfoClassName);
        instanceField.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC);

        MethodCreator staticInit = modelClass.getMethodCreator("<clinit>", void.class);
        staticInit.setModifiers(Opcodes.ACC_STATIC);
        staticInit.writeStaticField(instanceField.getFieldDescriptor(),
                staticInit.newInstance(MethodDescriptor.ofConstructor(modelInfoClassName)));
        staticInit.returnValue(null);

        // getEntityClass
        MethodCreator getEntityClass = modelClass.getMethodCreator("getEntityClass", Class.class);
        getEntityClass.returnValue(getEntityClass.loadClass(modelClassName));

        EntityField idField = getIdField(modelClassName, entities);
        createFromRow(modelClass, modelClassName, modelSignature, fields, idField);

        // insertStatement
        MethodCreator insertStatement = modelClass.getMethodCreator("insertStatement", String.class);
        StringBuilder names = new StringBuilder();
        StringBuilder indices = new StringBuilder();
        StringBuilder updateFieldNoId = new StringBuilder();
        int manyToOnes = 0;
        int fieldCount = 0;
        for (int i = 0; i < fields.size(); i++) {
            EntityField field = fields.get(i);
            // skip collections
            if (field.isOneToMany())
                continue;
            if (names.length() != 0) {
                names.append(", ");
                indices.append(", ");
            }
            if (updateFieldNoId.length() != 0) {
                updateFieldNoId.append(", ");
            }
            if (field.isManyToOne())
                manyToOnes++;
            names.append(field.columnName());
            // count this field, unlike relations or ignored fields
            fieldCount++;
            indices.append("$" + fieldCount);
            // FIXME: depends on ID being the first field
            if (i > 0) {
                updateFieldNoId.append(field.columnName() + " = $" + fieldCount);
            }
        }
        insertStatement
                .returnValue(insertStatement.load("INSERT INTO " + tableName + " (" + names + ") VALUES (" + indices + ")"));

        // updateStatement
        MethodCreator updateStatement = modelClass.getMethodCreator("updateStatement", String.class);
        updateStatement.returnValue(
                updateStatement.load("UPDATE " + tableName + " SET " + updateFieldNoId + " WHERE " + idField.name + " = $1"));

        // getTableName
        MethodCreator getTableName = modelClass.getMethodCreator("getTableName", String.class);
        getTableName.returnValue(getTableName.load(tableName));

        // toTuple
        createToTuple(modelClass, modelClassName, fields, manyToOnes, entities, idField);

        // Bridge methods
        MethodCreator toTupleBridge = modelClass.getMethodCreator("toTuple", CompletionStage.class, PanacheRxEntityBase.class);
        toTupleBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        toTupleBridge.returnValue(toTupleBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, "toTuple",
                CompletionStage.class, modelClassName),
                toTupleBridge.getThis(),
                toTupleBridge.checkCast(toTupleBridge.getMethodParam(0), modelClassName)));

        MethodCreator fromRowBridge = modelClass.getMethodCreator("fromRow", PanacheRxEntityBase.class, Row.class);
        fromRowBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        fromRowBridge.returnValue(fromRowBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, "fromRow",
                modelClassName, Row.class),
                fromRowBridge.getThis(),
                fromRowBridge.getMethodParam(0)));

        modelClass.close();
    }

    private static void collectFields(Map<String, EntityModel> entities, String modelClassName, List<EntityField> fields) {
        EntityModel entityModel = entities.get(modelClassName);
        if (entityModel == null)
            return;

        // collect supertype fields first
        if (entityModel.superClassName != null)
            collectFields(entities, entityModel.superClassName, fields);

        fields.addAll(entityModel.fields.values());
    }

    private static void createFromRow(ClassCreator modelClass, String modelClassName, String modelSignature,
            List<EntityField> fields, EntityField idField) {
        // fromRow
        MethodCreator fromRow = modelClass.getMethodCreator("fromRow", modelClassName, Row.class.getName());
        AssignableResultHandle variable = fromRow.createVariable(modelSignature);
        // arg-less constructor
        fromRow.assign(variable, fromRow.newInstance(MethodDescriptor.ofConstructor(modelClassName)));

        // set each field from the Row
        for (EntityField field : fields) {
            ResultHandle value;
            AssignableResultHandle fieldValue = fromRow.createVariable(field.typeDescriptor);
            if (field.isOneToMany()) {
                // fieldValue = RxOperations.deferPublisher(() -> RxDog.<RxDog>stream("owner_id = ?1", id));
                FunctionCreator deferred = fromRow.createFunction(Callable.class);
                BytecodeCreator deferredCreator = deferred.getBytecode();

                ResultHandle array = deferredCreator.newArray(Object.class, deferredCreator.load(1));
                deferredCreator.writeArrayValue(array, 0,
                        deferredCreator.readInstanceField(
                                FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                                variable));
                ResultHandle obs = deferredCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(field.entityClassName(), "stream", Publisher.class, String.class,
                                Object[].class),
                        // FIXME: do not hardcode
                        deferredCreator.load(field.reverseField + "_id = ?1"), array);
                deferredCreator.returnValue(obs);
                deferredCreator.close();
                // FIXME: ADD CACHE?
                fromRow.assign(fieldValue, fromRow.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "deferPublisher", Publisher.class, Callable.class),
                        deferred.getInstance()));
            } else {
                if (field.isEnum) {
                    ResultHandle enumValues = fromRow.invokeStaticMethod(MethodDescriptor.ofMethod(field.typeClassName(),
                            "values", "[L" + field.typeClassName() + ";"));

                    value = fromRow.invokeStaticMethod(MethodDescriptor.ofMethod(RxDataTypes.class, field.getFromRowMethod(), Enum.class,
                            Row.class, String.class, Enum[].class),
                            fromRow.getMethodParam(0), fromRow.load(field.columnName()), enumValues);
                } else if (field.isManyToOne()) {
                    value = fromRow.invokeStaticMethod(
                            MethodDescriptor.ofMethod(RxDataTypes.class, "getManyToOne", CompletionStage.class,
                                    Row.class, String.class, RxModelInfo.class),
                            fromRow.getMethodParam(0), fromRow.load(field.columnName()),
                            getModelInfo(fromRow, field.entityClassName()));
                } else {
                    value = fromRow.invokeStaticMethod(
                            MethodDescriptor.ofMethod(RxDataTypes.class, field.getFromRowMethod(), field.mappedTypeClassName(),
                                    Row.class, String.class),
                            fromRow.getMethodParam(0), fromRow.load(field.columnName()));
                }
                fromRow.assign(fieldValue, value);
            }
            fromRow.writeInstanceField(FieldDescriptor.of(modelClassName, field.name, field.typeDescriptor), variable,
                    fieldValue);
        }
        fromRow.returnValue(variable);
    }

    private static ResultHandle getModelInfo(MethodCreator creator, String entityClassName) {
        String modelInfoClassName = entityClassName + PanacheRxEntityEnhancer.RX_MODEL_SUFFIX;
        return creator.readStaticField(FieldDescriptor.of(modelInfoClassName,
                PanacheRxEntityEnhancer.RX_MODEL_FIELD_NAME,
                modelInfoClassName));
    }

    private static void createToTuple(ClassCreator modelClass, String modelClassName, List<EntityField> fields,
            int manyToOnes, Map<String, EntityModel> entities, EntityField idField) {
        MethodCreator toTuple = modelClass.getMethodCreator("toTuple", CompletionStage.class, modelClassName);
        ResultHandle entityParam = toTuple.getMethodParam(0);

        BytecodeCreator creator = toTuple;
        FunctionCreator myFunction = null;
        if (manyToOnes > 0) {
            myFunction = toTuple.createFunction(Function.class);
            creator = myFunction.getBytecode();
        }
        ResultHandle myTuple = creator.invokeStaticMethod(MethodDescriptor.ofMethod(Tuple.class, "tuple", Tuple.class));

        BranchResult branch = creator
                .ifNull(creator.readInstanceField(FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                        entityParam));
        branch.trueBranch().close();
        ResultHandle idFieldValue = branch.falseBranch().readInstanceField(
                FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                entityParam);
        branch.falseBranch().invokeVirtualMethod(MethodDescriptor.ofMethod(Tuple.class, "addValue", Tuple.class, Object.class),
                myTuple, idFieldValue);
        branch.falseBranch().close();

        // skip the ID field
        for (int j = 1, entityField = 0; j < fields.size(); j++) {
            EntityField field = fields.get(j);
            // skip collections
            if (field.isOneToMany())
                continue;
            ResultHandle fieldValue;
            if (field.isManyToOne()) {
                // we get the value from the function parameter
                // fieldValue = (($relationEntityClassName)((Object[])param)[{entityField++}]).id;
                String relationEntityClassName = field.entityClass.name().toString();
                EntityField relationEntityIdField = getIdField(relationEntityClassName, entities);
                // FIXME: wrong field type
                fieldValue = creator.readInstanceField(FieldDescriptor.of(PanacheRxEntity.class, relationEntityIdField.name,
                        relationEntityIdField.typeDescriptor),
                        creator.checkCast(
                                creator.readArrayValue(creator.checkCast(creator.getMethodParam(0), Object[].class),
                                        entityField++),
                                relationEntityClassName));
            } else {
                // fieldValue = entityParam.${field.name}
                fieldValue = creator.readInstanceField(FieldDescriptor.of(modelClassName, field.name, field.typeDescriptor),
                        entityParam);
            }
            String toTupleStoreMethod = field.getToTupleStoreMethod();
            if (toTupleStoreMethod != null)
                fieldValue = creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxDataTypes.class, toTupleStoreMethod, Object.class,
                                field.getToTupleStoreType()),
                        fieldValue);
            creator.invokeVirtualMethod(MethodDescriptor.ofMethod(Tuple.class, "addValue", Tuple.class, Object.class), myTuple,
                    fieldValue);
        }

        if (manyToOnes > 0) {
            // f = () -> ... myTuple
            creator.returnValue(myTuple);

            // CompletionStage[] myArgs = new CompletionStage[$manyToOnes]
            AssignableResultHandle myArgs = toTuple.createVariable(CompletionStage[].class);
            toTuple.assign(myArgs, toTuple.newArray(CompletionStage[].class, toTuple.load(manyToOnes)));
            int i = 0;
            for (EntityField field : fields) {
                if (!field.isManyToOne())
                    continue;
                // myArgs[$i++] = entityParam.${field.name};
                toTuple.writeArrayValue(myArgs, i++,
                        toTuple.readInstanceField(FieldDescriptor.of(modelClassName, field.name, field.typeDescriptor),
                                entityParam));
            }
            // return RxOperations.zipArray(f, myArgs)
            toTuple.returnValue(toTuple.invokeStaticMethod(
                    MethodDescriptor.ofMethod(RxOperations.class, "zipArray", CompletionStage.class, Function.class,
                            CompletionStage[].class),
                    myFunction.getInstance(), myArgs));
        } else {
            // return CompletableFuture.completedFuture(myTuple)
            creator.returnValue(
                    creator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(CompletableFuture.class, "completedFuture", CompletableFuture.class,
                                    Object.class),
                            myTuple));
        }
    }

    private static EntityField getIdField(String entityClassName, Map<String, EntityModel> entities) {
        EntityModel entityModel = entities.get(entityClassName);
        if (entityModel.idField != null)
            return entityModel.idField;
        if (entityModel.superClassName != null)
            return getIdField(entityModel.superClassName, entities);
        throw new RuntimeException("Failed to find ID field for entity " + entityClassName);
    }
}
