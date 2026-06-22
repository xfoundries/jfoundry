package org.jfoundry.infrastructure.messaging.externalization;

import org.jmolecules.event.types.DomainEvent;

/// 领域事件 Sink 扩展点。
/// <p>
/// 默认实现 {@code DomainEventExternalizer} 把 {@code @Externalized} 标记的事件
/// 序列化并写入 Outbox 表（同事务）。
public interface DomainEventSink {

    /// 处理单个领域事件。如果事件不匹配外部化规则，则忽略。
    void handle(DomainEvent event);
}
