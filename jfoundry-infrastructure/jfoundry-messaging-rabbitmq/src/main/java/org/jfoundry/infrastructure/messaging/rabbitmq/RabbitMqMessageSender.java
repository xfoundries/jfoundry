package org.jfoundry.infrastructure.messaging.rabbitmq;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/// RabbitMQ-backed {@link MessageSender}.
public class RabbitMqMessageSender implements MessageSender {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqMessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            rabbitTemplate.convertAndSend(topic, payloadKey, payload);
            return SendResult.ok();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return SendResult.fail(cause.getMessage());
        }
    }
}
