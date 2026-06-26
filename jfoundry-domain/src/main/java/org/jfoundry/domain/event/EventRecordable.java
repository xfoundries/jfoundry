package org.jfoundry.domain.event;

import org.jmolecules.event.types.DomainEvent;

import java.util.List;

/// 可记录领域事件的能力。
/// <p>
/// 表达聚合根在自身边界内已经产生的领域事件，由应用层在用例边界统一提取并分发。
/// 该接口与 jMolecules 的 AggregateRoot 解耦：jMolecules AggregateRoot 是纯标记，
/// events API 由 framework 自定义的此接口承载，BaseAggregateRoot 实现它。
public interface EventRecordable {

    /// 以单步移交语义提取并清空当前待分发的领域事件。
    ///
    /// 这里的“原子性”仅指在聚合根单线程使用模型内，调用方通过一次方法调用完成
    /// “读取当前事件 + 清空已读取事件”的 handoff 语义；它不表示任何并发或线程安全保证。
    /// 后置条件：返回后当前实例不再保留本次返回的事件；无事件时返回空列表。
    ///
    /// @return 当前待分发的领域事件快照，无事件时返回空列表
    List<DomainEvent> drainEvents();
}
