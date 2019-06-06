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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

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

    public void updateSchema(Set<String> annotatedClasses, String url, String user, String pass) {
        Map<String, String> settings = new HashMap<>();
        settings.put(Environment.DRIVER, "org.postgresql.Driver");
        settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL95Dialect");
        String jdbcUrl = "jdbc:"+url.substring("vertx-reactive:".length());
        settings.put(Environment.URL, jdbcUrl);
        settings.put(Environment.USER, user);
        settings.put(Environment.PASS, pass);
        settings.put(Environment.HBM2DDL_AUTO, "create");
        settings.put(Environment.SHOW_SQL, "true");
 
        MetadataSources metadata = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .applySettings(settings)
                        .build());
        for (String klass : annotatedClasses) {
            try {
                metadata.addAnnotatedClass(Class.forName(klass));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.setHaltOnError(true);
        schemaExport.setFormat(true);
        schemaExport.setDelimiter(";");
//        schemaExport.setOutputFile("db-schema.sql");
        schemaExport.create(EnumSet.of(TargetType.DATABASE), metadata.buildMetadata());
    }

}
