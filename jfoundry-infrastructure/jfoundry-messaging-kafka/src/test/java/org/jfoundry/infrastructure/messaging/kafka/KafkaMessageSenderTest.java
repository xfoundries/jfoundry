package org.jfoundry.infrastructure.messaging.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaMessageSenderTest {

    @SuppressWarnings("unchecked")
    private final Producer<String, String> producer = mock(Producer.class);
    private final KafkaMessageSender sender = new KafkaMessageSender(producer, Duration.ofSeconds(1));

    @Test
    void returnsOkWhenKafkaSendCompletes() {
        when(producer.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        SendResult result = sender.send("order.created", "order-1", "{}");

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();

        ArgumentCaptor<ProducerRecord<String, String>> record = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(record.capture());
        assertThat(record.getValue().topic()).isEqualTo("order.created");
        assertThat(record.getValue().key()).isEqualTo("order-1");
        assertThat(record.getValue().value()).isEqualTo("{}");
    }

    @Test
    void returnsFailureWhenKafkaSendFails() {
        CompletableFuture<RecordMetadata> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker down"));
        when(producer.send(any())).thenReturn(failed);

        SendResult result = sender.send("order.created", "order-1", "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("broker down");
    }
}
