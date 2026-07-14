package org.jfoundry.infrastructure.messaging.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jfoundry.application.messaging.PayloadSerializer;

/// Default Outbox payload serializer based on Jackson and JSR-310.
/// <p>
/// Disables {@code WRITE_DATES_AS_TIMESTAMPS} to emit ISO-8601 strings. It deliberately does not
/// enable Jackson default typing: integration payloads must not expose Java class names, and the
/// Outbox record already carries an explicit payload type alongside the JSON body.
/// <p>
/// Applications can replace serialization by registering their own {@link PayloadSerializer} bean.
public class JacksonPayloadSerializer implements PayloadSerializer {

    private final ObjectMapper objectMapper;

    public JacksonPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload: " + event.getClass().getName(), e);
        }
    }
}
