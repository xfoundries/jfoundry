package org.jfoundry.quarkus.messaging.rabbitmq.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import org.jfoundry.infrastructure.messaging.rabbitmq.quarkus.QuarkusRabbitMqMessageSender;

/// Registers the RabbitMQ MessageSender with Quarkus during augmentation.
class RabbitMqMessageSenderProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerRabbitMqMessageSender() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(QuarkusRabbitMqMessageSender.class)
                .setUnremovable()
                .build();
    }
}
