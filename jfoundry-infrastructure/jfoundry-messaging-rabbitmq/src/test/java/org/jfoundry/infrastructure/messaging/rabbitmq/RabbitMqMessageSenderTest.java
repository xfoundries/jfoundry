package org.jfoundry.infrastructure.messaging.rabbitmq;

import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqMessageSenderTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMqMessageSender sender = new RabbitMqMessageSender(rabbitTemplate);

    @Test
    void returnsOkWhenRabbitTemplateSendCompletes() {
        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        verify(rabbitTemplate).convertAndSend("order.exchange", "order.created", "{}");
    }

    @Test
    void returnsFailureWhenRabbitTemplateSendFails() {
        doThrow(new IllegalStateException("broker down"))
                .when(rabbitTemplate).convertAndSend("order.exchange", "order.created", "{}");

        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
