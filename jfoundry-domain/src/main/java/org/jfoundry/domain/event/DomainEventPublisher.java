package org.jfoundry.domain.event;

import org.jmolecules.event.types.DomainEvent;

/// 领域事件发布器接口
/// <p>
/// 定义在领域层，由基础设施层实现（如 Spring、Kafka 等）。
/// 领域层不限定事件基类，避免将核心模型绑定到具体消息框架。
/// 事务、异步、Outbox 和可靠投递策略由具体发布器实现决定。
public interface DomainEventPublisher {

    /// 发布领域事件。
    /// <p>
    /// 发布器只提供批量入口；单个事件也通过可变数组形式传入。
    /// 空事件数组应视为 no-op。
    /// 事件数组和数组中的事件都不允许为 null。
    ///
    /// @param events 事件数组
    void publish(DomainEvent... events);
}
