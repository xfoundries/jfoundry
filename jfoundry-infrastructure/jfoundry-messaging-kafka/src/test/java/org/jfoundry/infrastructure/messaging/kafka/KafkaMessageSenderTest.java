package org.jfoundry.infrastructure.messaging.kafka;

import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaMessageSenderTest {

    @SuppressWarnings("unchecked")
    private final KafkaOperations<String, String> kafkaOperations = mock(KafkaOperations.class);
    private final KafkaMessageSender sender = new KafkaMessageSender(kafkaOperations, Duration.ofSeconds(1));

    @Test
    void returnsOkWhenKafkaSendCompletes() {
        when(kafkaOperations.send("order.created", "order-1", "{}"))
                .thenReturn(CompletableFuture.completedFuture(null));

        SendResult result = sender.send("order.created", "order-1", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void returnsFailureWhenKafkaSendFails() {
        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failed =
                new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker down"));
        when(kafkaOperations.send("order.created", "order-1", "{}")).thenReturn(failed);

        SendResult result = sender.send("order.created", "order-1", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
