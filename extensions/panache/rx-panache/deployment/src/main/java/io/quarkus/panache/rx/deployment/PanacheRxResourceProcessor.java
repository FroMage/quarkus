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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.panache.rx.PanacheRxEntity;
import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.PanacheRxRepository;
import io.quarkus.panache.rx.PanacheRxRepositoryBase;
import io.quarkus.panache.rx.runtime.PgPoolProducer;
import io.reactiverse.pgclient.PgPool;

/**
 *
 */
public final class PanacheRxResourceProcessor {

    static final DotName DOTNAME_PANACHE_RX_REPOSITORY_BASE = DotName
            .createSimple(PanacheRxRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_RX_REPOSITORY = DotName.createSimple(PanacheRxRepository.class.getName());
    static final DotName DOTNAME_PANACHE_RX_ENTITY_BASE = DotName.createSimple(PanacheRxEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_RX_ENTITY = DotName.createSimple(PanacheRxEntity.class.getName());

    private static final Object DOTNAME_ARC_CLIENT_PROXY = DotName.createSimple(ClientProxy.class.getName());

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    DotName.createSimple(PgPoolProducer.class.getName()),
                    DotName.createSimple(PgPool.class.getName()))));

    @BuildStep
    AdditionalBeanBuildItem producePgPool() {
        return new AdditionalBeanBuildItem(PgPoolProducer.class);
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailible() {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                for (Type t : beanInfo.getTypes()) {
                    if (UNREMOVABLE_BEANS.contains(t.name())) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    @BuildStep
    void build(CombinedIndexBuildItem index,
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
            PanacheRxModelInfoGenerator.generateModelClass(rxModelClass, rxModelEnhancer.entities, generatedClasses);
            //            nonJpaModelBuildItems.produce(new NonJpaModelBuildItem(rxModelClass));
        }

        if (!rxModelEnhancer.entities.isEmpty()) {
            PanacheRxFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheRxFieldAccessEnhancer(
                    rxModelEnhancer.entities);
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!rxModelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
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
