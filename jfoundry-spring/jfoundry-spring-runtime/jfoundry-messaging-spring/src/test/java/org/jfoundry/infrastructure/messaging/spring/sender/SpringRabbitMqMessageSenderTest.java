package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringRabbitMqMessageSenderTest {

    private final RabbitOperations rabbitOperations = mock(RabbitOperations.class);
    private final SpringRabbitMqMessageSender sender = new SpringRabbitMqMessageSender(rabbitOperations);

    @Test
    void returnsOkWhenRabbitSendCompletes() {
        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        verify(rabbitOperations).convertAndSend("order.exchange", "order.created", "{}");
    }

    @Test
    void returnsFailureWhenRabbitSendFails() {
        doThrow(new IllegalStateException("broker down"))
                .when(rabbitOperations)
                .convertAndSend("order.exchange", "order.created", "{}");

        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
