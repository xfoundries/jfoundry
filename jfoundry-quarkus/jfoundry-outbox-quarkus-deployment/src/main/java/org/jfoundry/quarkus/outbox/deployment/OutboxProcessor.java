package org.jfoundry.quarkus.outbox.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import org.jfoundry.infrastructure.outbox.quarkus.QuarkusOutboxDispatcher;
import org.jfoundry.infrastructure.outbox.quarkus.QuarkusOutboxMaintenance;

/// Registers the Outbox dispatcher with Quarkus during augmentation.
class OutboxProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerOutboxRuntime() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(QuarkusOutboxDispatcher.class)
                .addBeanClass(QuarkusOutboxMaintenance.class)
                .setUnremovable()
                .build();
    }
}
