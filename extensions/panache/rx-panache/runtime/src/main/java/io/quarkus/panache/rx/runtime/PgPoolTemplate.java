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

package io.quarkus.panache.rx.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Template;

@Template
public class PgPoolTemplate {

    public void configureRuntimeProperties(PgPoolRuntimeConfig runtimeConfig) {
        // TODO @dmlloyd
        // Same here, the map is entirely empty (obviously, I didn't expect the values
        // that were not properly injected but at least the config objects present in
        // the map)
        // The elements from the default datasource are there
        Arc.container().instance(PgPoolProducer.class).get().setRuntimeConfig(runtimeConfig.defaultDataSource);
    }
}
