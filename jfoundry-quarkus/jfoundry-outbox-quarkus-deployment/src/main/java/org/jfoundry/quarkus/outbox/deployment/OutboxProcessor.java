package org.jfoundry.quarkus.outbox.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import org.jfoundry.infrastructure.outbox.quarkus.QuarkusOutboxDispatcher;

/// Registers the Outbox dispatcher with Quarkus during augmentation.
class OutboxProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerOutboxDispatcher() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(QuarkusOutboxDispatcher.class)
                .setUnremovable()
                .build();
    }
}
