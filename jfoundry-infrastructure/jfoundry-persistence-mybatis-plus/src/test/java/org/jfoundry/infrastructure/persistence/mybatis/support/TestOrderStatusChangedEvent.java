package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

/// 测试用领域事件:订单状态已变更。
public record TestOrderStatusChangedEvent(String orderId, String fromStatus, String toStatus, Instant occurredAt)
        implements DomainEvent {
}
