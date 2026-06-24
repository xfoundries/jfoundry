package org.jfoundry.infrastructure.outbox.spring.externalization;

import org.jfoundry.domain.event.AbstractDomainEvent;
import org.jfoundry.infrastructure.messaging.PayloadSerializer;
import org.jfoundry.infrastructure.messaging.externalization.AggregateRoutingMetadata;
import org.jfoundry.infrastructure.messaging.externalization.AggregateRoutingResolver;
import org.jfoundry.infrastructure.messaging.externalization.DomainEventSink;
import org.jfoundry.infrastructure.messaging.externalization.ExternalizationRule;
import org.jfoundry.infrastructure.messaging.externalization.ExternalizationRuleResolver;
import org.jfoundry.infrastructure.outbox.core.OutboxEntry;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

/// DomainEventSink 默认实现：把 {@code @Externalized} 标记的事件序列化并写入 Outbox 表（同事务）。
/// <p>
/// 本类只作为 Sink 被同步调用（由 SpringDomainEventPublisher.publish 转 dispatch 到 sink.handle），
/// 不再额外监听 ApplicationEvent —— 否则同一事件会被同步调用与监听器调用各处理一次，导致重复写 Outbox。
public class DomainEventExternalizer implements DomainEventSink {

    private final OutboxRepository outboxRepository;
    private final PayloadSerializer payloadSerializer;
    private final ExternalizationRuleResolver ruleResolver;
    private final AggregateRoutingResolver aggregateRoutingResolver;

    public DomainEventExternalizer(OutboxRepository outboxRepository,
                                    PayloadSerializer payloadSerializer,
                                    ExternalizationRuleResolver ruleResolver) {
        this(outboxRepository, payloadSerializer, ruleResolver, new AggregateRoutingResolver());
    }

    public DomainEventExternalizer(OutboxRepository outboxRepository,
                                    PayloadSerializer payloadSerializer,
                                    ExternalizationRuleResolver ruleResolver,
                                    AggregateRoutingResolver aggregateRoutingResolver) {
        this.outboxRepository = outboxRepository;
        this.payloadSerializer = payloadSerializer;
        this.ruleResolver = ruleResolver;
        this.aggregateRoutingResolver = aggregateRoutingResolver;
    }

    @Override
    public void handle(DomainEvent event) {
        ExternalizationRule rule = ruleResolver.resolve(event).orElse(null);
        if (rule == null) {
            return;
        }
        String eventId = resolveEventId(event);
        String payloadType = event.getClass().getName();
        String payloadJson = payloadSerializer.serialize(event);
        Instant occurredAt = resolveOccurredAt(event);
        AggregateRoutingMetadata aggregate = aggregateRoutingResolver.resolve(event).orElse(null);
        String payloadKey = rule.payloadKey();
        if (payloadKey == null && aggregate != null) {
            payloadKey = aggregate.aggregateId();
        }
        OutboxEntry entry = OutboxEntry.newPending(
                eventId,
                rule.topic(),
                payloadKey,
                payloadType,
                payloadJson,
                occurredAt,
                aggregate != null ? aggregate.aggregateType() : null,
                aggregate != null ? aggregate.aggregateId() : null,
                aggregate != null ? aggregate.aggregateVersion() : null);
        outboxRepository.append(entry);
    }

    private static String resolveEventId(DomainEvent event) {
        if (event instanceof AbstractDomainEvent abstractEvent) {
            return abstractEvent.getEventId().toString();
        }
        return java.util.UUID.randomUUID().toString();
    }

    private static Instant resolveOccurredAt(DomainEvent event) {
        if (event instanceof AbstractDomainEvent abstractEvent) {
            return abstractEvent.getOccurredAt();
        }
        return Instant.now();
    }
}
