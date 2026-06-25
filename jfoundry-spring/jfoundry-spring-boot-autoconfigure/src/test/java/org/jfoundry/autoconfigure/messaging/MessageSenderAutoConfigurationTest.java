package org.jfoundry.autoconfigure.messaging;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.spring.sender.LoggingMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSenderAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageSenderAutoConfiguration.class));

    @Test
    void registersLoggingSenderThatDoesNotReportDeliverySuccess() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MessageSender.class);
            assertThat(context.getBean(MessageSender.class)).isInstanceOf(LoggingMessageSender.class);

            SendResult result = context.getBean(MessageSender.class)
                    .send("orders", "order-1", "{\"event\":\"created\"}");

            assertThat(result.success()).isFalse();
        });
    }

    @Test
    void backsOffWhenUserProvidesMessageSender() {
        runner.withBean(MessageSender.class, () -> (topic, key, payload) -> SendResult.ok())
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageSender.class);
                    assertThat(context.getBean(MessageSender.class)).isNotInstanceOf(LoggingMessageSender.class);
                });
    }
}
