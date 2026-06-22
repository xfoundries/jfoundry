package org.jfoundry.domain.event;

import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/// 领域事件基类。
/// <p>
/// 业务事件继承此类即获得 occurredAt（事件发生时间）和 eventId（事件唯一标识）元数据，
/// 用于下游消费、审计追踪和幂等去重。
/// <p>
/// 本类为不可变：occurredAt 和 eventId 均为 final 字段，子类业务字段亦应声明为 final。
/// <p>
/// 幂等去重的权威键是 {@link #getEventId()}：消费方应基于 eventId 而非对象身份判断重复。
/// 本类的 equals/hashCode 同样基于 eventId，便于在 Set/Map 等集合场景做去重。
public abstract class AbstractDomainEvent implements DomainEvent {

    /// 事件发生时间。
    private final Instant occurredAt;

    /// 事件唯一标识。
    private final UUID eventId;

    protected AbstractDomainEvent() {
        this.occurredAt = Instant.now();
        this.eventId = UUID.randomUUID();
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractDomainEvent that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
