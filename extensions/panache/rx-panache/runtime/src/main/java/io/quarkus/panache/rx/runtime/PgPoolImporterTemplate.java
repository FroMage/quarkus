/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.quarkus.panache.rx.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.reactiverse.axle.pgclient.PgPool;

@Template
public class PgPoolImporterTemplate {

    public void configure(String importSql, BeanContainer container) {

        PgPool pool = container.instance(PgPool.class);
        InputStream sql = Thread.currentThread().getContextClassLoader().getResourceAsStream(importSql);
        if (sql != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sql, "UTF-8"))) {
                String line;
                String command = "";
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("--") || line.isEmpty())
                        continue;
                    command += line;
                    // only execute commands when ending with ;
                    if (line.endsWith(";")) {
                        pool.query(command).toCompletableFuture().get();
                        command = "";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

}
