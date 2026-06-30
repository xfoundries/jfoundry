package org.jfoundry.infrastructure.messaging.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        assertThat(json).contains("\"@class\":\"org.jfoundry.infrastructure.messaging.jackson.JacksonPayloadSerializerTest$SerializerTestEvent\"");
    }

    static class SerializerTestEvent {

        public Instant getOccurredAt() {
            return Instant.parse("2026-06-18T10:00:00Z");
        }
    }
}
