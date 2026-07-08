package org.jfoundry.infrastructure.messaging.rabbitmq;

import com.rabbitmq.client.Channel;
import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqMessageSenderTest {

    private final Channel channel = mock(Channel.class);
    private final RabbitMqMessageSender sender = new RabbitMqMessageSender(channel);

    @Test
    void returnsOkWhenRabbitPublishCompletes() throws Exception {
        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(channel).basicPublish(
                org.mockito.ArgumentMatchers.eq("order.exchange"),
                org.mockito.ArgumentMatchers.eq("order.created"),
                org.mockito.ArgumentMatchers.isNull(),
                body.capture());
        assertThat(new String(body.getValue(), StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    @Test
    void returnsFailureWhenRabbitPublishFails() throws Exception {
        doThrow(new IOException("broker down"))
                .when(channel).basicPublish(
                        org.mockito.ArgumentMatchers.eq("order.exchange"),
                        org.mockito.ArgumentMatchers.eq("order.created"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any(byte[].class));

        SendResult result = sender.send("order.exchange", "order.created", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
