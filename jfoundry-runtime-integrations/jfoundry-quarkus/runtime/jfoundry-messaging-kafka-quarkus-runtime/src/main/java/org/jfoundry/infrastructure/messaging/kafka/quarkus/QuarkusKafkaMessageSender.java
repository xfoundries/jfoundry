package org.jfoundry.infrastructure.messaging.kafka.quarkus;

import io.quarkus.arc.DefaultBean;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;

/// Quarkus Kafka-backed {@link MessageSender}.
@ApplicationScoped
@DefaultBean
public class QuarkusKafkaMessageSender implements MessageSender {

    private final MutinyEmitter<String> emitter;
    @Inject
    public QuarkusKafkaMessageSender(@Channel("jfoundry-kafka") MutinyEmitter<String> emitter) {
        this.emitter = emitter;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withTopic(topic)
                    .withKey(payloadKey)
                    .build();
            emitter.sendMessage(Message.of(payload).addMetadata(metadata)).await().indefinitely();
            return SendResult.ok();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return SendResult.fail(cause.getMessage());
        }
    }
}
