package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.springframework.amqp.rabbit.core.RabbitOperations;

/// Spring RabbitMQ-backed {@link MessageSender}.
public class SpringRabbitMqMessageSender implements MessageSender {

    private final RabbitOperations rabbitOperations;

    public SpringRabbitMqMessageSender(RabbitOperations rabbitOperations) {
        this.rabbitOperations = rabbitOperations;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            rabbitOperations.convertAndSend(topic, payloadKey, payload);
            return SendResult.ok();
        } catch (Exception exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            return SendResult.fail(cause.getMessage());
        }
    }
}
