package org.jfoundry.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.infrastructure.messaging.externalization.DomainEventSink;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.spring.externalization.DomainEventExternalizer;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// P1-2 regression: business-side custom DomainEventSink must NOT cause
/// framework's DomainEventExternalizer to retract. Externalizer is the tail
/// of the sink chain; it should only retract when business provides its own
/// DomainEventExternalizer.
/// <p>
/// TestApp provides an ObjectMapper bean because DomainEventExternalizerAutoConfiguration's
/// unconditional payloadSerializer bean requires Jackson (same pattern as
/// DomainEventPublisherAutoConfigurationTest). Marking payloadSerializer conditional
/// on ObjectMapper is out of Task 1.2 scope.
@SpringBootTest(classes = {
        DomainEventExternalizerConditionTest.TestApp.class,
        DomainEventExternalizerConditionTest.WithCustomSink.class
})
class DomainEventExternalizerConditionTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @TestConfiguration
    static class WithCustomSink {
        @Bean
        DomainEventSink loggingSink() {
            return new LoggingSink();
        }

        @Bean
        OutboxRepository stubOutboxRepository() {
            return new StubOutboxRepository();
        }
    }

    static class LoggingSink implements DomainEventSink {
        @Override
        public void handle(DomainEvent event) {
            // no-op
        }
    }

    /// OutboxRepository stub. DomainEventExternalizer only invokes
    /// {@link OutboxRepository#append(OutboxEntry)}; the rest satisfy the
    /// interface contract so the test compiles.
    static class StubOutboxRepository implements OutboxRepository {
        @Override
        public void append(OutboxEntry entry) {
            // no-op
        }

        @Override
        public List<OutboxEntry> findDispatchable(int limit, Instant now) {
            return List.of();
        }

        @Override
        public void markAsPublished(String eventId) {
            // no-op
        }

        @Override
        public void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff) {
            // no-op
        }

        @Override
        public void reactivate(String eventId) {
            // no-op
        }

        @Override
        public List<OutboxEntry> claimDispatchable(int limit, String claimerId) {
            return List.of();
        }

        @Override
        public int recoverStuckDispatching(java.time.Instant cutoff) {
            return 0;
        }
    }

    @Autowired
    private DomainEventExternalizer externalizer;

    @Test
    void externalizerStillRegisteredWhenCustomSinkExists() {
        assertThat(externalizer).isNotNull();
    }
}
