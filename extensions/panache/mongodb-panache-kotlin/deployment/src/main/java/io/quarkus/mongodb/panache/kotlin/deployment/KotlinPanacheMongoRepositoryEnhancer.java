package io.quarkus.mongodb.panache.kotlin.deployment;

import java.util.Collections;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor;

public class KotlinPanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public KotlinPanacheMongoRepositoryEnhancer(IndexView index, TypeBundle types) {
        super(index, types);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassOperationGenerationVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, bundle,
                bundle.repositoryBase(), Collections.emptyList());
    }
}
