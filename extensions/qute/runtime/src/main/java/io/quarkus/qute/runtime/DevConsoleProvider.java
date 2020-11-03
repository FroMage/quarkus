package io.quarkus.qute.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Conditionally registered in DEV mode for the DEV console
 */
@Singleton
public class DevConsoleProvider {

    static volatile DevQuteInfos devQuteInfos;

    public static class DevQuteInfos {
        private List<DevQuteTemplateInfo> templateInfos = new ArrayList<>();

        public DevQuteInfos() {
        }

        public List<DevQuteTemplateInfo> getTemplateInfos() {
            return templateInfos;
        }

        public void setTemplateInfos(List<DevQuteTemplateInfo> templateInfos) {
            this.templateInfos = templateInfos;
        }

        public void addQuteTemplateInfo(DevQuteTemplateInfo info) {
            templateInfos.add(info);
        }
    }

    public static class DevQuteTemplateInfo implements Comparable<DevQuteTemplateInfo> {

        private String path;
        private List<String> variants;
        private Map<String, String> parameters;

        public DevQuteTemplateInfo() {
        }

        public DevQuteTemplateInfo(String path, List<String> variants, Map<String, String> parameters) {
            this.path = path;
            this.variants = variants;
            this.parameters = parameters;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getVariants() {
            return variants;
        }

        public void setVariants(List<String> variants) {
            this.variants = variants;
        }

        @Override
        public int compareTo(DevQuteTemplateInfo o) {
            return path.compareTo(o.path);
        }
    }

    @Named("devQuteInfos")
    @ApplicationScoped
    public DevQuteInfos getDevQuteInfos() {
        return devQuteInfos;
    }
}
