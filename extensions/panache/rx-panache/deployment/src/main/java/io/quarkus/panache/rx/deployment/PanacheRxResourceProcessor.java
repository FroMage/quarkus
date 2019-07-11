/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.panache.rx.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.panache.rx.PanacheRxEntity;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.PanacheRxRepository;
import io.quarkus.panache.rx.PanacheRxRepositoryBase;
import io.quarkus.panache.rx.runtime.PgPoolImporterTemplate;
import io.quarkus.panache.rx.runtime.client.TransactionPgClients;
import io.quarkus.reactive.pg.client.deployment.PgPoolBuildItem;
import io.quarkus.reactive.pg.client.runtime.DataSourceConfig;

/**
 *
 */
public final class PanacheRxResourceProcessor {

    private static final String PANACHE_RX_CONFIG_PREFIX = "quarkus.panache-rx.";

    static final DotName DOTNAME_PANACHE_RX_REPOSITORY_BASE = DotName
            .createSimple(PanacheRxRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_RX_REPOSITORY = DotName.createSimple(PanacheRxRepository.class.getName());
    static final DotName DOTNAME_PANACHE_RX_ENTITY_BASE = DotName.createSimple(PanacheRxEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_RX_ENTITY = DotName.createSimple(PanacheRxEntity.class.getName());

    private static final Object DOTNAME_ARC_CLIENT_PROXY = DotName.createSimple(ClientProxy.class.getName());

    @BuildStep
    ModelClassesBuildItem build(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            // FIXME: put back to support jpa/rx mixes but move it out of hibernate-orm to not depend on it
            //            BuildProducer<NonJpaModelBuildItem> nonJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) throws Exception {

        PanacheRxRepositoryEnhancer rxRepositoryEnhancer = new PanacheRxRepositoryEnhancer(index.getIndex());
        Set<String> rxRepositoryClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_RX_REPOSITORY_BASE)) {
            // Skip PanacheRxRepository
            if (classInfo.name().equals(DOTNAME_PANACHE_RX_REPOSITORY))
                continue;
            // skip ArC proxies
            if (isArCProxy(classInfo))
                continue;
            rxRepositoryClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_RX_REPOSITORY)) {
            // skip ArC proxies
            if (isArCProxy(classInfo))
                continue;
            rxRepositoryClasses.add(classInfo.name().toString());
        }
        for (String rxRepositoryClass : rxRepositoryClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(rxRepositoryClass, rxRepositoryEnhancer));
        }

        PanacheRxEntityEnhancer rxModelEnhancer = new PanacheRxEntityEnhancer(index.getIndex());
        Set<String> rxModelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheEntity if we ask for subtypes of PanacheRxEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_RX_ENTITY_BASE)) {
            // FIXME: should we really skip PanacheRxEntity or all MappedSuperClass?
            if (classInfo.name().equals(DOTNAME_PANACHE_RX_ENTITY))
                continue;
            if (rxModelClasses.add(classInfo.name().toString()))
                rxModelEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_RX_ENTITY)) {
            if (rxModelClasses.add(classInfo.name().toString()))
                rxModelEnhancer.collectFields(classInfo);
        }
        // even though we don't augment it, we need to collect its id field
        ClassInfo panacheRxEntity = index.getIndex().getClassByName(DOTNAME_PANACHE_RX_ENTITY);
        rxModelEnhancer.collectFields(panacheRxEntity);

        for (String rxModelClass : rxModelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(rxModelClass, rxModelEnhancer));
            PanacheRxModelInfoGenerator.generateModelClass(rxModelClass, rxModelEnhancer.modelInfo, generatedClasses);
            //            nonJpaModelBuildItems.produce(new NonJpaModelBuildItem(rxModelClass));
        }

        if (rxModelEnhancer.modelInfo.hasEntities()) {
            PanacheRxFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheRxFieldAccessEnhancer(
                    rxModelEnhancer.modelInfo);
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!rxModelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }

        Set<String> allModelClasses = new HashSet<>(rxModelClasses);
        allModelClasses.add(PanacheRxEntity.class.getName());
        return new ModelClassesBuildItem(allModelClasses);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(TransactionPgClients.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configure(PanacheRxConfig config,
            LaunchModeBuildItem launchMode,
            DataSourceConfig dataSourceConfig,
            ModelClassesBuildItem modelClasses,
            PgPoolImporterTemplate template,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ArchiveRootBuildItem root,
            BeanContainerBuildItem beanContainer,
            BuildProducer<SubstrateResourceBuildItem> resourceProducer,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentProducer,
            // make sure we are executed after this one
            PgPoolBuildItem pgPoolBuildItem) {

        // sql-load-script
        // explicit file or default one
        String importFile = config.sqlLoadScript.orElse("import.sql"); //default Hibernate ORM file imported

        Optional<Path> loadScriptPath = Optional
                .ofNullable(applicationArchivesBuildItem.getRootArchive().getChildPath(importFile));

        // we enroll for hot deployment even if the file does not exist
        hotDeploymentProducer.produce(new HotDeploymentWatchedFileBuildItem(importFile));

        // enlist resource if present
        loadScriptPath
                .filter(path -> !Files.isDirectory(path))
                .ifPresent(path -> {
                    String resourceAsString = root.getPath().relativize(loadScriptPath.get()).toString();
                    resourceProducer.produce(new SubstrateResourceBuildItem(resourceAsString));
                    template.configure(importFile, beanContainer.getValue());
                });

        String generation = config.generation.orElseGet(() -> {
            switch (launchMode.getLaunchMode()) {
                case DEVELOPMENT:
                    return "update";
                case TEST:
                    return "create";
                default:
                    return "none";
            }
        });
        if (!generation.equals("none")) {
            // FIXME: same defaults as reactivepgclient
            template.updateSchema(modelClasses.modelClasses,
                    generation,
                    dataSourceConfig.url.orElse(null),
                    dataSourceConfig.username.orElse(null),
                    dataSourceConfig.password.orElse(null));
        }

        //raise exception if explicit file is not present (i.e. not the default)
        config.sqlLoadScript
                .filter(o -> !loadScriptPath.filter(path -> !Files.isDirectory(path)).isPresent())
                .ifPresent(
                        c -> {
                            throw new ConfigurationError(
                                    "Unable to find file referenced in '" + PANACHE_RX_CONFIG_PREFIX
                                            + "sql-load-script="
                                            + c + "'. Remove property or add file to your path.");
                        });
    }

    private boolean isArCProxy(ClassInfo classInfo) {
        return classInfo.interfaceNames().contains(DOTNAME_ARC_CLIENT_PROXY);
    }

    static final class ProcessorClassOutput implements ClassOutput {
        private final BuildProducer<GeneratedClassBuildItem> producer;

        ProcessorClassOutput(BuildProducer<GeneratedClassBuildItem> producer) {
            this.producer = producer;
        }

        @Override
        public void write(final String name, final byte[] data) {
            producer.produce(new GeneratedClassBuildItem(false, name, data));
        }

    }
}
