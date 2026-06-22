package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;

/// 测试用领域事件:订单已创建。
public record TestOrderCreatedEvent(String orderId, Instant occurredAt) implements DomainEvent {
}
