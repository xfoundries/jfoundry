package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingMessageSenderTest {

    @Test
    void returnsFailureBecauseLogOnlySenderDoesNotDeliverExternally() {
        LoggingMessageSender sender = new LoggingMessageSender();

        SendResult result = sender.send("orders", "order-1", "{\"event\":\"created\"}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("No MessageSender");
    }
}
