package io.quarkus.mongodb.panache.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassOperationGenerationVisitor;

public class PanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public PanacheMongoRepositoryEnhancer(IndexView index, TypeBundle typeBundle) {
        super(index, typeBundle);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheRepositoryClassOperationGenerationVisitor(className, outputClassVisitor, indexView, bundle);
    }

}
