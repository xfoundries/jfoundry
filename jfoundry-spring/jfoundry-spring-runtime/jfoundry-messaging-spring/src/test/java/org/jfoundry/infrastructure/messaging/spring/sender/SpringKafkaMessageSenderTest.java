package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringKafkaMessageSenderTest {

    @SuppressWarnings("unchecked")
    private final KafkaOperations<String, String> kafkaOperations = mock(KafkaOperations.class);
    private final SpringKafkaMessageSender sender = new SpringKafkaMessageSender(
            kafkaOperations,
            Duration.ofSeconds(1));

    @Test
    void returnsOkWhenKafkaSendCompletes() {
        when(kafkaOperations.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        SendResult result = sender.send("order.created", "order-1", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        verify(kafkaOperations).send("order.created", "order-1", "{}");
    }

    @Test
    void returnsFailureWhenKafkaSendFails() {
        when(kafkaOperations.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("broker down"));

        SendResult result = sender.send("order.created", "order-1", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
