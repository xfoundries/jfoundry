package org.jfoundry.quarkus.messaging.rabbitmq.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import org.jfoundry.infrastructure.messaging.rabbitmq.quarkus.QuarkusRabbitMqMessageSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqMessageSenderProcessorTest {

    @Test
    void registersTheRabbitMqMessageSender() {
        AdditionalBeanBuildItem beans = new RabbitMqMessageSenderProcessor().registerRabbitMqMessageSender();

        assertThat(beans.getBeanClasses()).contains(QuarkusRabbitMqMessageSender.class.getName());
        assertThat(beans.isRemovable()).isFalse();
    }
}
