package org.jfoundry.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.DotName;
import org.jfoundry.infrastructure.persistence.quarkus.QuarkusAggregatePersistenceContextBinder;
import org.jfoundry.infrastructure.persistence.quarkus.QuarkusAggregatePersistenceContext;
import org.jfoundry.infrastructure.transaction.quarkus.QuarkusTransactionRunner;

/// Registers JFoundry Quarkus runtime adapters during Quarkus augmentation.
class QuarkusProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerTransactionRunner() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(QuarkusTransactionRunner.class)
                .setUnremovable()
                .setDefaultScope(DotName.createSimple(ApplicationScoped.class.getName()))
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerAggregatePersistenceContext() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(QuarkusAggregatePersistenceContext.class)
                .addBeanClass(QuarkusAggregatePersistenceContextBinder.class)
                .setUnremovable()
                .setDefaultScope(DotName.createSimple(ApplicationScoped.class.getName()))
                .build();
    }
}
