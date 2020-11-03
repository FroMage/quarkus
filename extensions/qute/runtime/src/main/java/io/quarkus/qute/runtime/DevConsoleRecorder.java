package io.quarkus.qute.runtime;

import io.quarkus.qute.runtime.DevConsoleProvider.DevQuteInfos;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevConsoleRecorder {

    public void setDevQuteInfos(DevQuteInfos quteInfos) {
        DevConsoleProvider.devQuteInfos = quteInfos;
    }

}
