package io.quarkus.qute.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.qute.runtime.DevConsoleProvider;
import io.quarkus.qute.runtime.DevConsoleProvider.DevQuteInfos;
import io.quarkus.qute.runtime.DevConsoleProvider.DevQuteTemplateInfo;
import io.quarkus.qute.runtime.DevConsoleRecorder;
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
            List<CheckedTemplateBuildItem> checkedTemplates,
            TemplateVariantsBuildItem variants) {
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            DevQuteInfos quteInfos = new DevQuteInfos();
            for (CheckedTemplateBuildItem checkedTemplate : checkedTemplates) {
                DevQuteTemplateInfo templateInfo = new DevQuteTemplateInfo(checkedTemplate.templateId,
                        variants.getVariants().get(checkedTemplate.templateId),
                        checkedTemplate.bindings);
                quteInfos.addQuteTemplateInfo(templateInfo);
            }
            recorder.setDevQuteInfos(quteInfos);
        }
    }

}
