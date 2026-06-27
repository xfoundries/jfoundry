package org.jfoundry.autoconfigure.messaging.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.rocketmq.RocketMqMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RocketMqMessageSenderAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RocketMqMessageSenderAutoConfiguration.class,
                    MessageSenderAutoConfiguration.class))
            .withBean(DefaultMQProducer.class, () -> mock(DefaultMQProducer.class));

    @Test
    void createsRocketMqMessageSenderWhenProducerExists() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MessageSender.class);
            assertThat(context.getBean(MessageSender.class)).isInstanceOf(RocketMqMessageSender.class);
        });
    }

    @Test
    void backsOffWhenUserProvidesMessageSender() {
        runner.withBean(MessageSender.class, () -> (topic, key, payload) -> SendResult.ok())
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageSender.class);
                    assertThat(context).doesNotHaveBean(RocketMqMessageSender.class);
                });
    }

}
