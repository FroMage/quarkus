package io.quarkus.deployment.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.StorageReadyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.storage.QuarkusThread;
import io.quarkus.runtime.storage.StorageRecorder;
import io.smallrye.context.storage.spi.StorageDeclaration;

public class StorageBuildProcessor {

    public final class StorageFieldInfo {
        public final String typeSignature;
        public final String rawType;
        public final int index;
        public final Type type;
        public final ClassInfo threadLocalStorageClassInfo;

        public StorageFieldInfo(int index, Type type, ClassInfo threadLocalStorageClassInfo) {
            this.index = index;
            this.type = type;
            this.threadLocalStorageClassInfo = threadLocalStorageClassInfo;
            // type.name() is the raw type
            rawType = type.name().toString('.');
            typeSignature = AsmUtil.getSignature(type, v -> null);

        }
    }

    private static final String QUARKUS_STORAGE_IMPL_CLASS_NAME_PREFIX = "io.quarkus.deployment.storage.QuarkusStorageImpl__";
    private static final DotName DOTNAME_STORAGE_DECLARATION = DotName.createSimple(StorageDeclaration.class.getName());

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public StorageReadyBuildItem setupStorage(StorageRecorder recorder,
            RecorderContext recorderContext,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClass)
            throws ClassNotFoundException, IOException {

        List<StorageFieldInfo> fields = new ArrayList<>();
        int index = 0;

        for (ClassInfo threadLocalStorageClassInfo : combinedIndex.getIndex()
                .getAllKnownImplementors(DOTNAME_STORAGE_DECLARATION)) {
            Type storageType = null;
            for (Type superClassType : threadLocalStorageClassInfo.interfaceTypes()) {
                if (superClassType.kind() != Kind.PARAMETERIZED_TYPE)
                    continue;
                ParameterizedType parameterizedType = superClassType.asParameterizedType();
                if (!parameterizedType.name().equals(DOTNAME_STORAGE_DECLARATION))
                    continue;
                if (parameterizedType.arguments().size() != 1)
                    throw storageValidation(threadLocalStorageClassInfo);
                storageType = parameterizedType.arguments().get(0);
                break;
            }
            if (storageType == null)
                throw storageValidation(threadLocalStorageClassInfo);
            System.err.println("Adding context element for " + threadLocalStorageClassInfo + " -> " + index);
            fields.add(new StorageFieldInfo(index++,
                    storageType,
                    threadLocalStorageClassInfo));
        }

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        Map<String, RuntimeValue<ThreadLocal<?>>> storageMappings = new HashMap<>();

        // now create our ThreadLocal per field
        for (StorageFieldInfo storageFieldInfo : fields) {
            String className = QUARKUS_STORAGE_IMPL_CLASS_NAME_PREFIX + storageFieldInfo.index;
            System.err.println("Producing " + className + " for index " + storageFieldInfo.index);
            try (ClassCreator clazz = ClassCreator.builder().classOutput(classOutput)
                    .className(className)
                    .superClass(ThreadLocal.class)
                    .build()) {
                // Ljava/lang/ThreadLocal<Ljava/util/List<Ljava/util/Map<Ljava/lang/Class<*>;Ljava/lang/Object;>;>;>;
                clazz.setSignature("L" + ThreadLocal.class.getName().replace('.', '/') + "<"
                        + storageFieldInfo.typeSignature + ">;");

                try (MethodCreator method = clazz.getMethodCreator("get", storageFieldInfo.rawType)) {
                    method.setModifiers(Opcodes.ACC_PUBLIC);
                    // signature: ()Ljava/util/List<Ljava/util/Map<Ljava/lang/Class<*>;Ljava/lang/Object;>;>;
                    method.setSignature("()" + storageFieldInfo.typeSignature);
                    // GENERATED:
                    // public List<Map<Class<?>, Object>> get() {
                    //     Thread currentThread = Thread.currentThread();
                    //     if (currentThread instanceof QuarkusThread) {
                    //         return (List)((QuarkusThread) currentThread).getQuarkusThreadContext()[index];
                    //     } else {
                    //         return super.get();
                    //     }
                    // }
                    AssignableResultHandle threadVariable = method.createVariable(Thread.class);
                    method.assign(threadVariable,
                            method.invokeStaticMethod(MethodDescriptor.ofMethod(Thread.class, "currentThread", Thread.class)));
                    BranchResult test = method.ifNonZero(method.instanceOf(threadVariable, QuarkusThread.class));
                    try (BytecodeCreator ifTrue = test.trueBranch()) {
                        ResultHandle baseContexts = ifTrue.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(QuarkusThread.class, "getQuarkusThreadContext",
                                        Object[].class),
                                ifTrue.checkCast(threadVariable, QuarkusThread.class));
                        ResultHandle fieldContext = ifTrue.checkCast(
                                ifTrue.readArrayValue(baseContexts, storageFieldInfo.index),
                                storageFieldInfo.rawType);
                        ifTrue.returnValue(fieldContext);
                    }
                    try (BytecodeCreator ifFalse = test.falseBranch()) {
                        ResultHandle val = ifFalse.invokeSpecialMethod(
                                MethodDescriptor.ofMethod(ThreadLocal.class, "get", Object.class), ifFalse.getThis());
                        ifFalse.returnValue(ifFalse.checkCast(val, storageFieldInfo.rawType));
                    }
                }
                // bridge
                try (MethodCreator method = clazz.getMethodCreator("get", Object.class)) {
                    method.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC);
                    method.returnValue(
                            method.invokeVirtualMethod(MethodDescriptor.ofMethod(className, "get", storageFieldInfo.rawType),
                                    method.getThis()));
                }
                try (MethodCreator method = clazz.getMethodCreator("set", void.class, storageFieldInfo.rawType)) {
                    method.setModifiers(Opcodes.ACC_PUBLIC);
                    // signature: (Ljava/util/List<Ljava/util/Map<Ljava/lang/Class<*>;Ljava/lang/Object;>;>;)V
                    method.setSignature("(" + storageFieldInfo.typeSignature + ")V");
                    // GENERATED
                    // public void set(List<Map<Class<?>, Object>> t) {
                    //     Thread currentThread = Thread.currentThread();
                    //     if (currentThread instanceof QuarkusThread) {
                    //         ((QuarkusThread) currentThread).getQuarkusThreadContext()[index] = t;
                    //     } else {
                    //         super.set(t);
                    //     }
                    // }
                    AssignableResultHandle threadVariable = method.createVariable(Thread.class);
                    method.assign(threadVariable,
                            method.invokeStaticMethod(MethodDescriptor.ofMethod(Thread.class, "currentThread", Thread.class)));
                    BranchResult test = method.ifNonZero(method.instanceOf(threadVariable, QuarkusThread.class));
                    try (BytecodeCreator ifTrue = test.trueBranch()) {
                        ResultHandle baseContexts = ifTrue.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(QuarkusThread.class, "getQuarkusThreadContext",
                                        Object[].class),
                                ifTrue.checkCast(threadVariable, QuarkusThread.class));
                        ifTrue.writeArrayValue(baseContexts, storageFieldInfo.index, ifTrue.getMethodParam(0));
                    }
                    try (BytecodeCreator ifFalse = test.falseBranch()) {
                        ifFalse.invokeSpecialMethod(
                                MethodDescriptor.ofMethod(ThreadLocal.class, "set", void.class, Object.class),
                                ifFalse.getThis(), ifFalse.getMethodParam(0));
                    }
                    method.returnValue(method.loadNull());
                }
                // bridge
                try (MethodCreator method = clazz.getMethodCreator("set", void.class, Object.class)) {
                    method.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC);
                    method.returnValue(method.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(className, "set", void.class, storageFieldInfo.rawType),
                            method.getThis(),
                            method.checkCast(method.getMethodParam(0), storageFieldInfo.rawType)));
                    method.returnValue(method.loadNull());
                }
                try (MethodCreator method = clazz.getMethodCreator("remove", void.class)) {
                    method.setModifiers(Opcodes.ACC_PUBLIC);
                    // GENERATED
                    // public void remove() {
                    //     Thread currentThread = Thread.currentThread();
                    //     if (currentThread instanceof QuarkusThread) {
                    //         ((QuarkusThread) currentThread).getQuarkusThreadContext()[index] = null;
                    //     } else {
                    //         super.remove();
                    //     }
                    // }
                    AssignableResultHandle threadVariable = method.createVariable(Thread.class);
                    method.assign(threadVariable,
                            method.invokeStaticMethod(MethodDescriptor.ofMethod(Thread.class, "currentThread", Thread.class)));
                    BranchResult test = method.ifNonZero(method.instanceOf(threadVariable, QuarkusThread.class));
                    try (BytecodeCreator ifTrue = test.trueBranch()) {
                        ResultHandle baseContexts = ifTrue.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(QuarkusThread.class, "getQuarkusThreadContext",
                                        Object[].class),
                                ifTrue.checkCast(threadVariable, QuarkusThread.class));
                        ifTrue.writeArrayValue(baseContexts, storageFieldInfo.index, ifTrue.loadNull());
                    }
                    try (BytecodeCreator ifFalse = test.falseBranch()) {
                        ifFalse.invokeSpecialMethod(MethodDescriptor.ofMethod(ThreadLocal.class, "remove", void.class),
                                ifFalse.getThis());
                    }
                    method.returnValue(method.loadNull());
                }
            }

            storageMappings.put(storageFieldInfo.threadLocalStorageClassInfo.name().toString(),
                    recorderContext.newInstance(className));
        }

        recorder.configureStaticInit(storageMappings);

        return new StorageReadyBuildItem();
    }

    private RuntimeException storageValidation(ClassInfo threadLocalStorageClassInfo) {
        return new IllegalStateException(
                "ThreadLocalStorage class must be a non-raw class implementing StorageDeclaration: "
                        + threadLocalStorageClassInfo);
    }
}
