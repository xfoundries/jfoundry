package org.jfoundry.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.domain.event.AbstractDomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonPayloadSerializerTest {

    private final JacksonPayloadSerializer serializer = new JacksonPayloadSerializer(new ObjectMapper());

    @Test
    void serializesInstantFieldsAsIso8601() {
        SerializerTestEvent event = new SerializerTestEvent();

        String json = serializer.serialize(event);

        assertThat(json).contains("\"occurredAt\":\"2026-06-18T10:00:00Z\"");
        assertThat(json).contains("\"@class\":\"org.jfoundry.infrastructure.messaging.JacksonPayloadSerializerTest$SerializerTestEvent\"");
    }

    static class SerializerTestEvent extends AbstractDomainEvent {
        public SerializerTestEvent() {
            super();
        }
        @Override
        public Instant getOccurredAt() {
            return Instant.parse("2026-06-18T10:00:00Z");
        }
    }
}
