package io.quarkus.panache.rx.deployment;

import java.util.ArrayList;
import java.util.List;
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
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.PanacheRxQuery;
import io.quarkus.panache.rx.RxModelInfo;
import io.quarkus.panache.rx.deployment.PanacheRxResourceProcessor.ProcessorClassOutput;
import io.quarkus.panache.rx.runtime.RxDataTypes;
import io.quarkus.panache.rx.runtime.RxOperations;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.axle.pgclient.Tuple;

public class PanacheRxModelInfoGenerator {

    public static void generateModelClass(String modelClassName, ModelInfo modelInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        List<EntityField> fields = new ArrayList<>();
        EntityModel entityModel = collectFields(modelInfo, modelClassName, fields);

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

        EntityField idField = entityModel.getIdField();
        createFromRow(modelClass, modelClassName, modelSignature, fields, idField);

        createIdMethods(modelClass, modelClassName, modelSignature, fields, idField);

        // insertStatement
        MethodCreator insertStatement = modelClass.getMethodCreator("insertStatement", String.class);
        StringBuilder names = new StringBuilder();
        StringBuilder indices = new StringBuilder();
        StringBuilder updateFieldNoId = new StringBuilder();
        int owningRelations = 0;
        int fieldCount = 0;
        for (int i = 0; i < fields.size(); i++) {
            EntityField field = fields.get(i);
            // skip collections and non-owning relations
            if (field.isNonOwningRelation() || field.isManyToMany())
                continue;
            if (names.length() != 0) {
                names.append(", ");
                indices.append(", ");
            }
            if (updateFieldNoId.length() != 0) {
                updateFieldNoId.append(", ");
            }
            if (field.isOwningRelation())
                owningRelations++;
            names.append(field.columnName());
            // count this field, unlike relations or ignored fields
            fieldCount++;
            indices.append("$" + fieldCount);
            // FIXME: cannot update field id with this logic
            if (!field.isId) {
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
        createToTuple(modelClass, modelClassName, fields, owningRelations, idField);

        // afterSave
        createAfterSave(modelClass, modelClassName, fields, idField);

        // beforeDelete
        createBeforeDelete(modelClass, modelClassName, fields, idField);

        // Bridge methods
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "toTuple", CompletionStage.class, PanacheRxEntityBase.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "afterSave", CompletionStage.class, PanacheRxEntityBase.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "beforeDelete", CompletionStage.class, PanacheRxEntityBase.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.Return, "fromRow", PanacheRxEntityBase.class, Row.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "getId", Object.class, PanacheRxEntityBase.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "setId", void.class, PanacheRxEntityBase.class, Object.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "isPersistent", boolean.class, PanacheRxEntityBase.class);
        generateBridge(modelClass, modelInfoClassName, modelClassName, BridgeType.FirstParam, "markPersistent", void.class, PanacheRxEntityBase.class);

        modelClass.close();
    }

    enum BridgeType {
        Return, FirstParam;
    }
    
    private static void generateBridge(ClassCreator modelClass, String modelInfoClassName, String modelClassName,
                                       BridgeType bridgeType,
                                       String methodName, Class<?> returnClass, Class<?>... params) {
        MethodCreator fromRowBridge = modelClass.getMethodCreator(methodName, returnClass, params);
        fromRowBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        ResultHandle[] passedParams = new ResultHandle[params.length];
        Object[] targetParamTypes = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            passedParams[i] = fromRowBridge.getMethodParam(i);
            if(bridgeType == BridgeType.FirstParam && i == 0) {
                passedParams[i] = fromRowBridge.checkCast(passedParams[i], modelClassName);
                targetParamTypes[i] = modelClassName;
            } else {
                targetParamTypes[i] = params[i];
            }
        }
        Object targetReturnType = bridgeType == BridgeType.Return ? modelClassName : returnClass;
        
        fromRowBridge.returnValue(fromRowBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, methodName,
                targetReturnType, targetParamTypes),
                fromRowBridge.getThis(),
                passedParams));
    }

    private static void createIdMethods(ClassCreator modelClass, String modelClassName, String modelSignature, List<EntityField> fields,
                                        EntityField idField) {
        SimpleTypeMapper typeMapper = idField.getTypeMapper();

        {
            MethodCreator getId = modelClass.getMethodCreator("getId", Object.class, modelClassName);
            ResultHandle readField = getId.readInstanceField(FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor), getId.getMethodParam(0));
            // FIXME: should it go via the accessor method?
            if(typeMapper.getGetIdMethod() == null)
                getId.returnValue(readField);
            else {
                getId.returnValue(getId.invokeStaticMethod(MethodDescriptor.ofMethod(RxDataTypes.class, typeMapper.getGetIdMethod(), 
                                                                                     Object.class, idField.typeDescriptor),
                                                           readField));
            }
            getId.close();
        }
        
        {
            MethodCreator setId = modelClass.getMethodCreator("setId", void.class, modelClassName, Object.class);
            // FIXME: should it go via the accessor method?
            ResultHandle convertedId = setId.invokeStaticMethod(MethodDescriptor.ofMethod(RxDataTypes.class, typeMapper.getSetIdMethod(), 
                                                                                          idField.typeDescriptor, Object.class),
                                                                setId.getMethodParam(1));
            setId.writeInstanceField(FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor), setId.getMethodParam(0), convertedId);
            setId.returnValue(setId.loadNull());
            setId.close();
        }

        {
            MethodCreator isPersistent = modelClass.getMethodCreator("isPersistent", boolean.class, modelClassName);
            isPersistent.returnValue(isPersistent.readInstanceField(FieldDescriptor.of(modelClassName, PanacheRxEntityEnhancer.RX_PERSISTENT_FIELD_NAME, boolean.class), isPersistent.getMethodParam(0)));
            isPersistent.close();

            MethodCreator markPersistent = modelClass.getMethodCreator("markPersistent", void.class, modelClassName);
            markPersistent.writeInstanceField(FieldDescriptor.of(modelClassName, PanacheRxEntityEnhancer.RX_PERSISTENT_FIELD_NAME, boolean.class), markPersistent.getMethodParam(0), markPersistent.load(true));
            markPersistent.returnValue(markPersistent.loadNull());
            markPersistent.close();
        }

        {
            MethodCreator isGeneratedId = modelClass.getMethodCreator("isGeneratedId", boolean.class);
            isGeneratedId.returnValue(isGeneratedId.load(idField.isGenerated));
            isGeneratedId.close();
        }

        {
            MethodCreator getGeneratorSequence = modelClass.getMethodCreator("getGeneratorSequence", String.class);
            if(idField.isGenerated) {
                getGeneratorSequence.returnValue(getGeneratorSequence.load(idField.generatorSequence()));
            } else {
                getGeneratorSequence.returnValue(getGeneratorSequence.loadNull());
            }
            getGeneratorSequence.close();
        }

        {
            MethodCreator getIdName = modelClass.getMethodCreator("getIdName", String.class);
            getIdName.returnValue(getIdName.load(idField.name));
            getIdName.close();
        }
    }

    private static void createBeforeDelete(ClassCreator modelClass, String modelClassName, List<EntityField> fields, EntityField idField) {
        MethodCreator beforeDelete = modelClass.getMethodCreator("beforeDelete", CompletionStage.class, modelClassName);
        AssignableResultHandle ret = beforeDelete.createVariable(CompletionStage.class);

        //  CompletionStage<Void> ret = CompletableFuture.completedFuture(null);
        beforeDelete.assign(ret,
                beforeDelete.invokeStaticMethod(
                        MethodDescriptor.ofMethod(CompletableFuture.class, "completedFuture", CompletableFuture.class,
                                Object.class),
                        beforeDelete.loadNull()));

        for (EntityField field : fields) {
            if (field.isManyToMany() && field.isOwningRelation()) {
                String joinTable = field.joinTable();
                String joinColumn = field.joinColumn();
                // deleteQuery: "DELETE FROM RxRelationEntity_RxManyToManyEntity WHERE relations_id = $1"
                String deleteQuery = "DELETE FROM " + joinTable + " WHERE " + joinColumn + " = $1";
                // ret = ret.thenCompose(v -> RxOperations.deleteManyToMany(param.id, deleteQuery));
                FunctionCreator functionCreator = beforeDelete.createFunction(Function.class);
                BytecodeCreator functionBytecode = functionCreator.getBytecode();
                ResultHandle deleteOperation = functionBytecode.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "deleteManyToMany", CompletionStage.class,
                                Object.class, String.class),
                        functionBytecode.readInstanceField(FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                                beforeDelete.getMethodParam(0)),
                        functionBytecode.load(deleteQuery));
                functionBytecode.returnValue(deleteOperation);
                functionBytecode.close();

                beforeDelete.assign(ret,
                        beforeDelete.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(CompletionStage.class, "thenCompose", CompletionStage.class,
                                        Function.class),
                                ret, functionCreator.getInstance()));
            }
        }

        beforeDelete.returnValue(ret);
        beforeDelete.close();
    }

    private static void createAfterSave(ClassCreator modelClass, String modelClassName, List<EntityField> fields, EntityField idField) {
        MethodCreator afterSave = modelClass.getMethodCreator("afterSave", CompletionStage.class, modelClassName);

        AssignableResultHandle ret = afterSave.createVariable(CompletionStage.class);

        //  CompletionStage<Void> ret = CompletableFuture.completedFuture(null);
        afterSave.assign(ret,
                afterSave.invokeStaticMethod(
                        MethodDescriptor.ofMethod(CompletableFuture.class, "completedFuture", CompletableFuture.class,
                                Object.class),
                        afterSave.loadNull()));

        for (EntityField field : fields) {
            if (field.isManyToMany() && field.isOwningRelation()) {
                String joinTable = field.joinTable();
                String joinColumn = field.joinColumn();
                String inverseJoinColumn = field.inverseJoinColumn();
                // deleteQuery: "DELETE FROM RxRelationEntity_RxManyToManyEntity WHERE relations_id = $1"
                String deleteQuery = "DELETE FROM " + joinTable + " WHERE " + joinColumn + " = $1";
                // insertQuery: "INSERT INTO RxRelationEntity_RxManyToManyEntity (relations_id, manytomanys_id) VALUES ($1, $2)"
                String insertQuery = "INSERT INTO " + joinTable + " (" + joinColumn + "," + inverseJoinColumn
                        + ") VALUES ($1, $2)";
                // ret = ret.thenCompose(v -> RxOperations.saveManyToMany(param.id, param.manyToManys, deleteQuery, insertQuery));
                FunctionCreator functionCreator = afterSave.createFunction(Function.class);
                BytecodeCreator functionBytecode = functionCreator.getBytecode();
                ResultHandle saveOperation = functionBytecode.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "saveManyToMany", CompletionStage.class,
                                Object.class, Publisher.class, String.class, String.class),
                        functionBytecode.readInstanceField(FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                                afterSave.getMethodParam(0)),
                        functionBytecode.readInstanceField(FieldDescriptor.of(modelClassName, field.name, Publisher.class),
                                afterSave.getMethodParam(0)),
                        functionBytecode.load(deleteQuery), functionBytecode.load(insertQuery));
                functionBytecode.returnValue(saveOperation);
                functionBytecode.close();

                afterSave.assign(ret,
                        afterSave.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(CompletionStage.class, "thenCompose", CompletionStage.class,
                                        Function.class),
                                ret, functionCreator.getInstance()));
            }
        }

        afterSave.returnValue(ret);
        afterSave.close();
    }

    private static EntityModel collectFields(ModelInfo modelInfo, String modelClassName, List<EntityField> fields) {
        EntityModel entityModel = modelInfo.getEntityModel(modelClassName);
        if (entityModel == null)
            return null;

        // collect supertype fields first
        if (entityModel.superClassName != null)
            collectFields(modelInfo, entityModel.superClassName, fields);

        fields.addAll(entityModel.fields.values());
        return entityModel;
    }

    private static void createFromRow(ClassCreator modelClass, String modelClassName, String modelSignature,
            List<EntityField> fields, EntityField idField) {
        // fromRow
        MethodCreator fromRow = modelClass.getMethodCreator("fromRow", modelClassName, Row.class.getName());
        AssignableResultHandle variable = fromRow.createVariable(modelSignature);
        // arg-less constructor
        fromRow.assign(variable, fromRow.newInstance(MethodDescriptor.ofConstructor(modelClassName)));
        // mark it persistent
        fromRow.writeInstanceField(FieldDescriptor.of(modelClassName, PanacheRxEntityEnhancer.RX_PERSISTENT_FIELD_NAME, boolean.class), variable, fromRow.load(true));

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
                        deferredCreator.load(field.inverseJoinColumn() + " = ?1"), array);
                deferredCreator.returnValue(obs);
                deferredCreator.close();
                // FIXME: ADD CACHE?
                fromRow.assign(fieldValue, fromRow.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "deferPublisher", Publisher.class, Callable.class),
                        deferred.getInstance()));
            } else if (field.isOneToOneNonOwning()) {
                // fieldValue = RxOperations.deferCompletionStage(() -> RxDog.<RxDog>find("owner_id = ?1", id).singleResult());
                FunctionCreator deferred = fromRow.createFunction(Callable.class);
                BytecodeCreator deferredCreator = deferred.getBytecode();

                ResultHandle array = deferredCreator.newArray(Object.class, deferredCreator.load(1));
                deferredCreator.writeArrayValue(array, 0,
                        deferredCreator.readInstanceField(
                                FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                                variable));
                ResultHandle obs = deferredCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(field.entityClassName(), "find", PanacheRxQuery.class, String.class,
                                Object[].class),
                        deferredCreator.load(field.inverseJoinColumn() + " = ?1"), array);
                obs = deferredCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(PanacheRxQuery.class, "singleResult", Object.class), obs);
                deferredCreator.returnValue(obs);
                deferredCreator.close();
                // FIXME: ADD CACHE?
                fromRow.assign(fieldValue, fromRow.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "deferCompletionStage", CompletionStage.class,
                                Callable.class),
                        deferred.getInstance()));
            } else if (field.isManyToMany()) {
                // fieldValue = RxOperations.deferPublisher(() -> RxOperations.findManyToMany(OWNER_MODEL_INFO, OTHER_MODEL_INFO, 
                //                                                                            ownerId, joinTable, joinColumn, inverseJoinColumn));
                FunctionCreator deferred = fromRow.createFunction(Callable.class);
                BytecodeCreator deferredCreator = deferred.getBytecode();

                ResultHandle ownerId = deferredCreator.readInstanceField(
                        FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                        variable);
                ResultHandle otherModelInfo = getModelInfo(deferredCreator, field.entityClassName());
                // reverse joinColumn/inverseJoinColumn for non-owning sides
                String joinColumn, inverseJoinColumn;
                if (field.isOwningRelation()) {
                    joinColumn = field.joinColumn();
                    inverseJoinColumn = field.inverseJoinColumn();
                } else {
                    inverseJoinColumn = field.joinColumn();
                    joinColumn = field.inverseJoinColumn();
                }
                ResultHandle obs = deferredCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "findManyToMany",
                                Publisher.class, RxModelInfo.class, RxModelInfo.class,
                                Object.class, String.class, String.class, String.class),
                        fromRow.getThis(), otherModelInfo, ownerId,
                        deferredCreator.load(field.joinTable()),
                        deferredCreator.load(joinColumn),
                        deferredCreator.load(inverseJoinColumn));
                deferredCreator.returnValue(obs);
                deferredCreator.close();

                // FIXME: ADD CACHE?
                fromRow.assign(fieldValue, fromRow.invokeStaticMethod(
                        MethodDescriptor.ofMethod(RxOperations.class, "deferPublisher", Publisher.class,
                                Callable.class),
                        deferred.getInstance()));
            } else {
                ResultHandle columnName = fromRow.load(field.columnName());
                if (field.isEnum) {
                    ResultHandle enumValues = fromRow.invokeStaticMethod(MethodDescriptor.ofMethod(field.typeClassName(),
                            "values", "[L" + field.typeClassName() + ";"));

                    value = fromRow.invokeStaticMethod(
                            MethodDescriptor.ofMethod(RxDataTypes.class, field.getFromRowMethod(), Enum.class,
                                    Row.class, String.class, Enum[].class),
                            fromRow.getMethodParam(0), columnName, enumValues);
                } else if (field.isOwningRelation()) {
                    value = fromRow.invokeStaticMethod(
                            MethodDescriptor.ofMethod(RxDataTypes.class, "getManyToOne", CompletionStage.class,
                                    Row.class, String.class, RxModelInfo.class),
                            fromRow.getMethodParam(0), columnName,
                            getModelInfo(fromRow, field.entityClassName()));
                } else {
                    value = fromRow.invokeStaticMethod(
                            MethodDescriptor.ofMethod(RxDataTypes.class, field.getFromRowMethod(), field.mappedTypeClassName(),
                                    Row.class, String.class),
                            fromRow.getMethodParam(0), columnName);
                }
                fromRow.assign(fieldValue, value);
            }
            fromRow.writeInstanceField(FieldDescriptor.of(modelClassName, field.name, field.typeDescriptor), variable,
                    fieldValue);
        }
        fromRow.returnValue(variable);
    }

    private static ResultHandle getModelInfo(BytecodeCreator creator, String entityClassName) {
        String modelInfoClassName = entityClassName + PanacheRxEntityEnhancer.RX_MODEL_SUFFIX;
        return creator.readStaticField(FieldDescriptor.of(modelInfoClassName,
                PanacheRxEntityEnhancer.RX_MODEL_FIELD_NAME,
                modelInfoClassName));
    }

    private static void createToTuple(ClassCreator modelClass, String modelClassName, List<EntityField> fields,
            int owningRelations, EntityField idField) {
        MethodCreator toTuple = modelClass.getMethodCreator("toTuple", CompletionStage.class, modelClassName);
        ResultHandle entityParam = toTuple.getMethodParam(0);

        BytecodeCreator creator = toTuple;
        FunctionCreator myFunction = null;
        if (owningRelations > 0) {
            myFunction = toTuple.createFunction(Function.class);
            creator = myFunction.getBytecode();
        }
        ResultHandle myTuple = creator.invokeStaticMethod(MethodDescriptor.ofMethod(Tuple.class, "tuple", Tuple.class));

        BranchResult branch = creator
                .ifNonZero(creator.readInstanceField(FieldDescriptor.of(modelClassName, PanacheRxEntityEnhancer.RX_PERSISTENT_FIELD_NAME, "Z"),
                                                     entityParam));
        ResultHandle idFieldValue = branch.trueBranch().readInstanceField(
                FieldDescriptor.of(modelClassName, idField.name, idField.typeDescriptor),
                entityParam);
        branch.trueBranch().invokeVirtualMethod(MethodDescriptor.ofMethod(Tuple.class, "addValue", Tuple.class, Object.class),
                myTuple, idFieldValue);
        branch.trueBranch().close();
        branch.falseBranch().close();

        for (int j = 0, entityField = 0; j < fields.size(); j++) {
            EntityField field = fields.get(j);
            // skip id, collections and non-owning 1-1
            if (field.isNonOwningRelation() || field.isManyToMany() || field.isId)
                continue;
            ResultHandle fieldValue;
            if (field.isOwningRelation()) {
                // we get the value from the function parameter
                // fieldValue = (($relationEntityClassName)((Object[])param)[{entityField++}]).id;
                String relationEntityClassName = field.entityClass.name().toString();
                EntityField relationEntityIdField = field.getInverseEntity().getIdField();
                fieldValue = creator.readInstanceField(FieldDescriptor.of(relationEntityClassName, relationEntityIdField.name,
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

        if (owningRelations > 0) {
            // f = () -> ... myTuple
            creator.returnValue(myTuple);

            // CompletionStage[] myArgs = new CompletionStage[$manyToOnes]
            AssignableResultHandle myArgs = toTuple.createVariable(CompletionStage[].class);
            toTuple.assign(myArgs, toTuple.newArray(CompletionStage[].class, toTuple.load(owningRelations)));
            int i = 0;
            for (EntityField field : fields) {
                if (!field.isOwningRelation() || field.isManyToMany())
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
}
