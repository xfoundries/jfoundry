package org.jfoundry.infrastructure.outbox.spring.externalization;

import org.jfoundry.infrastructure.messaging.PayloadSerializer;
import org.jfoundry.infrastructure.messaging.externalization.AggregateRouting;
import org.jfoundry.infrastructure.messaging.externalization.AggregateRoutingResolver;
import org.jfoundry.infrastructure.messaging.externalization.ExternalizationRuleResolver;
import org.jfoundry.infrastructure.messaging.externalization.MessageRouting;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.OutboxEntry;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jfoundry.infrastructure.outbox.core.OutboxStatus;
import org.jmolecules.event.annotation.Externalized;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventExternalizerTest {

    private final CapturingOutboxRepository repository = new CapturingOutboxRepository();
    private final DomainEventExternalizer externalizer = new DomainEventExternalizer(
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
        externalizer.handle(new OrderCreatedEvent());

        OutboxEntry entry = repository.lastAppended;
        assertThat(entry.getTopic()).isEqualTo("order.created");
        assertThat(entry.getPayloadKey()).isEqualTo("order-1");
        assertThat(entry.getAggregateType()).isEqualTo("Order");
        assertThat(entry.getAggregateId()).isEqualTo("order-1");
        assertThat(entry.getAggregateVersion()).isEqualTo(7L);
    }

    @Test
    void explicitPayloadKeyWinsOverAggregateId() {
        externalizer.handle(new ExplicitKeyEvent());

        OutboxEntry entry = repository.lastAppended;
        assertThat(entry.getPayloadKey()).isEqualTo("tenant-1");
        assertThat(entry.getAggregateId()).isEqualTo("order-2");
    }

    static class CapturingOutboxRepository implements OutboxRepository {
        private OutboxEntry lastAppended;

        @Override
        public void append(OutboxEntry entry) {
            this.lastAppended = entry;
        }

        @Override
        public List<OutboxEntry> findDispatchable(int limit, Instant now) {
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
        public List<OutboxEntry> claimDispatchable(int limit, String claimerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int recoverStuckDispatching(Instant cutoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteByStatusAndOccurredAtBefore(OutboxStatus status, Instant cutoff, int batchSize) {
            throw new UnsupportedOperationException();
        }
    }
}
