package org.jfoundry.application.messaging;

/// Outbound message sending abstraction.
public interface MessageSender {

    /// @param topic target topic, usually from {@code @MessageRouting} or {@code @Externalized.value}
    /// @param payloadKey routing key, possibly null; concrete adapters decide whether to use it
    /// @param payload serialized payload string
    /// @return send result
    SendResult send(String topic, String payloadKey, String payload);
}
