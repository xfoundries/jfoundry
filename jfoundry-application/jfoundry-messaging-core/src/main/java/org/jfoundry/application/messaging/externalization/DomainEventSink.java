package org.jfoundry.application.messaging.externalization;

import org.jmolecules.event.types.DomainEvent;

/// 领域事件外部化 Sink：作为事件链路的处理节点。
/// <p>
/// 框架支持多个 {@code DomainEventSink} 共存，按 Spring {@code @Order} 升序执行。
/// 业务侧可注册自定义 Sink（日志、metrics 等）补充链路。
/// <p>
/// 框架内置的 {@code DomainEventExternalizer} 是链路末端（{@code Ordered.LOWEST_PRECEDENCE}），
/// 负责把事件写入 Outbox 表。业务侧若注册自己的 {@code DomainEventExternalizer}，
/// 框架默认实现会自动退让。
public interface DomainEventSink {

    /// 处理单个领域事件。如果事件不匹配外部化规则，则忽略。
    void handle(DomainEvent event);
}
