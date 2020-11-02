package io.quarkus.arc.runtime;

import io.quarkus.arc.runtime.DevConsoleProvider.DevBeanInfos;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevConsoleRecorder {

    public void setDevBeanInfos(DevBeanInfos beanInfos) {
        DevConsoleProvider.devBeanInfos = beanInfos;
    }

}
