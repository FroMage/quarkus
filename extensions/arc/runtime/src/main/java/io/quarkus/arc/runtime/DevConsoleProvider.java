package io.quarkus.arc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;

import io.quarkus.arc.impl.ArcContainerImpl;

/**
 * Conditionally registered in DEV mode for the DEV console
 */
@Singleton
public class DevConsoleProvider {

    static volatile DevBeanInfos devBeanInfos;

    // FIXME: this should be a template extension method for String
    public static class ClassName {
        private String name;

        public ClassName() {
        }

        public ClassName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLocalName() {
            int lastDot = name.lastIndexOf('.');
            if (lastDot != -1) {
                return name.substring(lastDot + 1);
            }
            return name;
        }
    }

    public static class DevBeanInfos {
        private List<DevBeanInfo> beanInfos = new ArrayList<>();
        private List<DevBeanInfo> removedBeanInfos = new ArrayList<>();

        public DevBeanInfos() {
        }

        public void setRemovedBeanInfos(List<DevBeanInfo> removedBeanInfos) {
            this.removedBeanInfos = removedBeanInfos;
        }

        public List<DevBeanInfo> getRemovedBeanInfos() {
            Collections.sort(removedBeanInfos);
            return removedBeanInfos;
        }

        public List<DevBeanInfo> getBeanInfos() {
            Collections.sort(beanInfos);
            return beanInfos;
        }

        public void setBeanInfos(List<DevBeanInfo> beanInfos) {
            this.beanInfos = beanInfos;
        }

        public void addBeanInfo(DevBeanInfo beanInfo) {
            beanInfos.add(beanInfo);
        }

        public void addRemovedBeanInfo(DevBeanInfo beanInfo) {
            removedBeanInfos.add(beanInfo);
        }
    }

    public static enum DevBeanKind {
        METHOD,
        FIELD,
        CLASS,
        SYNTHETIC;
    }

    public static class DevBeanInfo implements Comparable<DevBeanInfo> {

        private ClassName name;
        private String methodName;
        private DevBeanKind kind;
        private ClassName type;
        private List<ClassName> qualifiers = new ArrayList<>();
        private ClassName scope;

        public DevBeanInfo() {
        }

        public DevBeanInfo(ClassName name, String methodName, ClassName type, List<ClassName> qualifiers, ClassName scope,
                DevBeanKind kind) {
            this.name = name;
            this.methodName = methodName;
            this.type = type;
            this.qualifiers = qualifiers;
            this.scope = scope;
            this.kind = kind;
        }

        public void setKind(DevBeanKind kind) {
            this.kind = kind;
        }

        public DevBeanKind getKind() {
            return kind;
        }

        public void setScope(ClassName scope) {
            this.scope = scope;
        }

        public ClassName getScope() {
            return scope;
        }

        public List<ClassName> getQualifiers() {
            return qualifiers;
        }

        public void setQualifiers(List<ClassName> qualifiers) {
            this.qualifiers = qualifiers;
        }

        public void setType(ClassName type) {
            this.type = type;
        }

        public ClassName getType() {
            return type;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public void setName(ClassName name) {
            this.name = name;
        }

        public ClassName getName() {
            return name;
        }

        @Override
        public int compareTo(DevBeanInfo o) {
            return type.getLocalName().compareTo(o.type.getLocalName());
        }
    }

    @Named("arcContainer")
    @ApplicationScoped
    public ArcContainerImpl getArcContainer() {
        return ArcContainerImpl.instance();
    }

    @Named("devBeanInfos")
    @ApplicationScoped
    public DevBeanInfos getDevBeanInfos() {
        return devBeanInfos;
    }
}
