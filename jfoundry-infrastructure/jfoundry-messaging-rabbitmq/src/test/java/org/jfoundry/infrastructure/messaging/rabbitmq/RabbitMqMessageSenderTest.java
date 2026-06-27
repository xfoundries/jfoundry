package org.jfoundry.infrastructure.messaging.rabbitmq;

import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqMessageSenderTest {

    private final RabbitOperations rabbitOperations = mock(RabbitOperations.class);
    private final RabbitMqMessageSender sender = new RabbitMqMessageSender(rabbitOperations);

    @Test
    void returnsOkWhenRabbitTemplateSendCompletes() {
        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        verify(rabbitOperations).convertAndSend("order.exchange", "order.created", "{}");
    }

    @Test
    void returnsFailureWhenRabbitTemplateSendFails() {
        doThrow(new IllegalStateException("broker down"))
                .when(rabbitOperations).convertAndSend("order.exchange", "order.created", "{}");

        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
