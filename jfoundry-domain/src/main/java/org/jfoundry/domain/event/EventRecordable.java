package org.jfoundry.domain.event;

import org.jmolecules.event.types.DomainEvent;

import java.util.List;

/// 可记录领域事件的能力。
/// <p>
/// 表达聚合根在自身边界内已经产生的领域事件，由 Repository 在持久化后移交发布器。
/// 该接口与 jMolecules 的 AggregateRoot 解耦：jMolecules AggregateRoot 是纯标记，
/// events API 由 framework 自定义的此接口承载，BaseAggregateRoot 实现它。
public interface EventRecordable {

    /// 获取当前待发布的领域事件。
    ///
    /// @return 领域事件列表，无事件时返回空列表
    List<DomainEvent> getEvents();

    /// 清空已移交发布器的领域事件。
    /// <p>
    /// 后置条件：调用返回后 {@link #getEvents()} 返回空列表。无事件时调用为 no-op。
    void clearEvents();
}
