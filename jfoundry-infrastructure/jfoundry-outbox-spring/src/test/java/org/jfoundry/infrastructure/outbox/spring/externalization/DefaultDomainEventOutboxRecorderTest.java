package org.jfoundry.infrastructure.outbox.spring.externalization;

import org.jfoundry.application.messaging.PayloadSerializer;
import org.jfoundry.application.event.externalization.AggregateRouting;
import org.jfoundry.application.event.externalization.AggregateRoutingResolver;
import org.jfoundry.application.event.externalization.ExternalizationRuleResolver;
import org.jfoundry.application.event.externalization.MessageRouting;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.application.outbox.OutboxMessageStatus;
import org.jmolecules.event.annotation.Externalized;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDomainEventOutboxRecorderTest {

    private final CapturingOutboxMessageStore repository = new CapturingOutboxMessageStore();
    private final DefaultDomainEventOutboxRecorder outboxRecorder = new DefaultDomainEventOutboxRecorder(
            repository,
            event -> "{}",
            new ExternalizationRuleResolver(),
            new AggregateRoutingResolver());

    @Externalized("order.created")
    @AggregateRouting(type = "Order", id = "orderId", version = "version")
    static class OrderCreatedEvent implements DomainEvent {
        public String getOrderId() {
            return "order-1";
        }

        public long getVersion() {
            return 7L;
        }
    }

    @Externalized("order.created")
    @MessageRouting(topic = "order.created", key = "tenantId")
    @AggregateRouting(type = "Order", id = "orderId")
    static class ExplicitKeyEvent implements DomainEvent {
        public String getTenantId() {
            return "tenant-1";
        }

        public String getOrderId() {
            return "order-2";
        }
    }

    @Test
    void storesAggregateMetadataAndUsesAggregateIdAsFallbackPayloadKey() {
        outboxRecorder.record(List.of(new OrderCreatedEvent()));

        OutboxMessage entry = repository.lastAppended;
        assertThat(entry.getTopic()).isEqualTo("order.created");
        assertThat(entry.getPayloadKey()).isEqualTo("order-1");
        assertThat(entry.getAggregateType()).isEqualTo("Order");
        assertThat(entry.getAggregateId()).isEqualTo("order-1");
        assertThat(entry.getAggregateVersion()).isEqualTo(7L);
    }

    @Test
    void explicitPayloadKeyWinsOverAggregateId() {
        outboxRecorder.record(List.of(new ExplicitKeyEvent()));

        OutboxMessage entry = repository.lastAppended;
        assertThat(entry.getPayloadKey()).isEqualTo("tenant-1");
        assertThat(entry.getAggregateId()).isEqualTo("order-2");
    }

    static class CapturingOutboxMessageStore implements OutboxMessageStore {
        private OutboxMessage lastAppended;

        @Override
        public void append(OutboxMessage entry) {
            this.lastAppended = entry;
        }

        @Override
        public List<OutboxMessage> findDispatchable(int limit, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markAsPublished(String eventId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reactivate(String eventId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OutboxMessage> claimDispatchable(int limit, String claimerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int recoverStuckDispatching(Instant cutoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteByStatusAndOccurredAtBefore(OutboxMessageStatus status, Instant cutoff, int batchSize) {
            throw new UnsupportedOperationException();
        }
    }
}
