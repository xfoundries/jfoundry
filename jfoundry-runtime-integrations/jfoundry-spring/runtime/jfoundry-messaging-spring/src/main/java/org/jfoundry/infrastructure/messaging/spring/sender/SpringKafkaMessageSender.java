package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.springframework.kafka.core.KafkaOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/// Spring Kafka-backed {@link MessageSender}.
public class SpringKafkaMessageSender implements MessageSender {

    private final KafkaOperations<String, String> kafkaOperations;
    private final Duration sendTimeout;

    public SpringKafkaMessageSender(KafkaOperations<String, String> kafkaOperations, Duration sendTimeout) {
        this.kafkaOperations = kafkaOperations;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            kafkaOperations.send(topic, payloadKey, payload).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return SendResult.ok();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SendResult.fail(exception.getMessage());
        } catch (Exception exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            return SendResult.fail(cause.getMessage());
        }
    }
}
