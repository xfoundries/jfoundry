package org.jfoundry.autoconfigure.messaging.rabbitmq;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.rabbitmq.RabbitMqMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMqMessageSenderAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RabbitMqMessageSenderAutoConfiguration.class,
                    MessageSenderAutoConfiguration.class))
            .withBean(RabbitOperations.class, () -> mock(RabbitOperations.class));

    @Test
    void createsRabbitMqMessageSenderWhenRabbitOperationsExists() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MessageSender.class);
            assertThat(context.getBean(MessageSender.class)).isInstanceOf(RabbitMqMessageSender.class);
        });
    }

    @Test
    void backsOffWhenUserProvidesMessageSender() {
        runner.withBean(MessageSender.class, () -> (topic, key, payload) -> SendResult.ok())
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageSender.class);
                    assertThat(context).doesNotHaveBean(RabbitMqMessageSender.class);
                });
    }

}
