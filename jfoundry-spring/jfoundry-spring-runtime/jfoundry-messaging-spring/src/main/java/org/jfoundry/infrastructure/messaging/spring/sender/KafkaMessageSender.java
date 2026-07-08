package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.springframework.kafka.core.KafkaOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/// Spring Kafka-backed {@link MessageSender}.
public class KafkaMessageSender implements MessageSender {

    private final KafkaOperations<String, String> kafkaOperations;
    private final Duration sendTimeout;

    public KafkaMessageSender(KafkaOperations<String, String> kafkaOperations, Duration sendTimeout) {
        this.kafkaOperations = kafkaOperations;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            kafkaOperations.send(topic, payloadKey, payload)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return SendResult.ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendResult.fail(e.getMessage());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return SendResult.fail(cause.getMessage());
        }
    }
}
