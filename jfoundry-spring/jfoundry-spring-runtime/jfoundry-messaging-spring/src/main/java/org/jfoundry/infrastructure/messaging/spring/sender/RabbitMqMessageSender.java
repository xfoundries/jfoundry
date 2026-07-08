package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.springframework.amqp.rabbit.core.RabbitOperations;

/// Spring RabbitMQ-backed {@link MessageSender}.
public class RabbitMqMessageSender implements MessageSender {

    private final RabbitOperations rabbitOperations;

    public RabbitMqMessageSender(RabbitOperations rabbitOperations) {
        this.rabbitOperations = rabbitOperations;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            rabbitOperations.convertAndSend(topic, payloadKey, payload);
            return SendResult.ok();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return SendResult.fail(cause.getMessage());
        }
    }
}
