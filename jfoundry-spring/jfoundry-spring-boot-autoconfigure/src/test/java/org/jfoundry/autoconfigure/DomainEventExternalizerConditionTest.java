package org.jfoundry.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.application.messaging.externalization.DomainEventSink;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.outbox.spring.externalization.DomainEventExternalizer;
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
/// TestApp provides an ObjectMapper bean so DomainEventExternalizerAutoConfiguration's
/// payloadSerializer（@ConditionalOnBean(ObjectMapper.class)）能正常注册，
/// 让 DomainEventExternalizer 的依赖链完整。
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
        OutboxMessageStore stubOutboxMessageStore() {
            return new StubOutboxMessageStore();
        }
    }

    static class LoggingSink implements DomainEventSink {
        @Override
        public void handle(DomainEvent event) {
            // no-op
        }
    }

    /// OutboxMessageStore stub. DomainEventExternalizer only invokes
    /// {@link OutboxMessageStore#append(OutboxMessage)}; the rest satisfy the
    /// interface contract so the test compiles.
    static class StubOutboxMessageStore implements OutboxMessageStore {
        @Override
        public void append(OutboxMessage entry) {
            // no-op
        }

        @Override
        public List<OutboxMessage> findDispatchable(int limit, Instant now) {
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
        public List<OutboxMessage> claimDispatchable(int limit, String claimerId) {
            return List.of();
        }

        @Override
        public int recoverStuckDispatching(java.time.Instant cutoff) {
            return 0;
        }

        @Override
        public int deleteByStatusAndOccurredAtBefore(
                org.jfoundry.application.outbox.OutboxMessageStatus status,
                java.time.Instant cutoff, int batchSize) {
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
