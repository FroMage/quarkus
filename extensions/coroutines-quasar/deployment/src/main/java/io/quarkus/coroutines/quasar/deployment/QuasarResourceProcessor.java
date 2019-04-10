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

package io.quarkus.coroutines.quasar.deployment;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import com.github.fromage.quasi.fibers.Suspendable;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

/**
 *
 */
public final class QuasarResourceProcessor {

    private static final DotName DOTNAME_SUSPENDABLE = DotName.createSimple(Suspendable.class.getName());

    @BuildStep
    void build(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers) throws Exception {

        QuasarEnhancer quasiEnhancer = new QuasarEnhancer();
        Set<String> quasiClasses = new HashSet<>();
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(DOTNAME_SUSPENDABLE)) {
            String quasiUserClassName = annotationInstance.target().asMethod().declaringClass().name().toString();
            quasiClasses.add(quasiUserClassName);
        }
        for (String quasiClass : quasiClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(quasiClass, quasiEnhancer));
        }
    }

}
