package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.runtime.DevConsoleProvider;
import io.quarkus.arc.runtime.DevConsoleProvider.ClassName;
import io.quarkus.arc.runtime.DevConsoleProvider.DevBeanInfo;
import io.quarkus.arc.runtime.DevConsoleProvider.DevBeanInfos;
import io.quarkus.arc.runtime.DevConsoleProvider.DevBeanKind;
import io.quarkus.arc.runtime.DevConsoleRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class DevConsoleProcessor {
    @BuildStep
    public AdditionalBeanBuildItem registerArcContainer(LaunchModeBuildItem launchMode) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            return AdditionalBeanBuildItem.unremovableOf(DevConsoleProvider.class);
        }
        return null;
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    public void collectBeanInfo(DevConsoleRecorder recorder,
            LaunchModeBuildItem launchMode,
            ValidationPhaseBuildItem validationPhaseBuildItem) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            DevBeanInfos beanInfos = new DevBeanInfos();
            for (BeanInfo beanInfo : validationPhaseBuildItem.getContext().beans().collect()) {
                beanInfos.addBeanInfo(makeBeanInfo(beanInfo));
            }
            for (BeanInfo beanInfo : validationPhaseBuildItem.getContext().removedBeans().collect()) {
                beanInfos.addRemovedBeanInfo(makeBeanInfo(beanInfo));
            }
            recorder.setDevBeanInfos(beanInfos);
        }
    }

    private DevBeanInfo makeBeanInfo(BeanInfo beanInfo) {
        List<ClassName> qualifiers = new ArrayList<>();
        for (AnnotationInstance qualAnnotation : beanInfo.getQualifiers()) {
            qualifiers.add(new ClassName(qualAnnotation.name().toString()));
        }
        ClassName scope = new ClassName(beanInfo.getScope().getDotName().toString());
        Optional<AnnotationTarget> target = beanInfo.getTarget();
        if (target.isPresent()) {
            AnnotationTarget annotated = target.get();
            String methodName = null;
            ClassName type = null;
            ClassName providerClass = null;
            DevBeanKind kind = null;
            if (annotated.kind() == Kind.METHOD) {
                MethodInfo method = annotated.asMethod();
                methodName = method.name();
                type = new ClassName(method.returnType().toString());
                providerClass = new ClassName(method.declaringClass().toString());
                kind = DevBeanKind.METHOD;
            } else if (annotated.kind() == Kind.FIELD) {
                FieldInfo field = annotated.asField();
                methodName = field.name();
                type = new ClassName(field.type().toString());
                providerClass = new ClassName(field.declaringClass().toString());
                kind = DevBeanKind.FIELD;
            } else if (annotated.kind() == Kind.CLASS) {
                ClassInfo klass = annotated.asClass();
                type = new ClassName(klass.name().toString());
                kind = DevBeanKind.CLASS;
            }
            return new DevBeanInfo(providerClass, methodName, type, qualifiers, scope, kind);
        } else {
            return new DevBeanInfo(null, null, new ClassName(beanInfo.getBeanClass().toString()),
                    qualifiers,
                    scope, DevBeanKind.SYNTHETIC);
        }
    }
}
