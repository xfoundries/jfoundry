package org.jfoundry.infrastructure.messaging.rocketmq;

import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/// RocketMQ-backed {@link MessageSender}.
public class RocketMqMessageSender implements MessageSender {

    private final MQProducer producer;
    private final Duration sendTimeout;

    public RocketMqMessageSender(MQProducer producer, Duration sendTimeout) {
        this.producer = producer;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            Message message = new Message(topic, payload.getBytes(StandardCharsets.UTF_8));
            if (payloadKey != null) {
                message.setKeys(payloadKey);
            }
            producer.send(message, sendTimeout.toMillis());
            return SendResult.ok();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return SendResult.fail(cause.getMessage());
        }
    }
}
