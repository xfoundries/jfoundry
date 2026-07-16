package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/// Persists Instants in UTC through SQL TIMESTAMP columns without timezone information.
@Converter(autoApply = false)
public final class InstantUtcConverter implements AttributeConverter<Instant, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : LocalDateTime.ofInstant(attribute, ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime databaseValue) {
        return databaseValue == null ? null : databaseValue.toInstant(ZoneOffset.UTC);
    }
}
