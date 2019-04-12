package io.quarkus.panache.rx.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;

@ApplicationScoped
public class PgPoolProducer {

    private DataSourceRuntimeConfig runtimeConfig;

    @ApplicationScoped
    @Produces
    public io.reactiverse.pgclient.PgPool getClient() {
        PgPoolOptions options = new PgPoolOptions()
                .setHost(runtimeConfig.host.get())
                .setDatabase(runtimeConfig.database.get())
                .setUser(runtimeConfig.username.get())
                .setPassword(runtimeConfig.password.get());
        options.setMaxSize(runtimeConfig.maxSize);
        options.setPort(runtimeConfig.port);

        // Create the client pool
        PgPool pool = PgClient.pool(options);

        // FIXME: make configurable?
        InputStream sql = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/load.sql");
        if (sql != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sql, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("--") || line.isEmpty())
                        continue;
                    // FIXME: multi-line SQL?
                    pool.query(line).toCompletableFuture().get();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        return pool.getDelegate();
    }

    public void setRuntimeConfig(DataSourceRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public DataSourceRuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }
}
