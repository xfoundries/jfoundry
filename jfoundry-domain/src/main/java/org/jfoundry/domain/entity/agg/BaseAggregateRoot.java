package org.jfoundry.domain.entity.agg;

import org.jfoundry.domain.event.EventRecordable;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.event.types.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/// 聚合根基类。
///
/// 提供聚合根的通用实现：
/// - 标识符管理。
/// - 领域事件记录和提取。
/// - jMolecules AggregateRoot 标记 + EventRecordable 能力。
/// <p>
/// 聚合根只负责在自身边界内记录已发生的领域事件；应用层应在用例边界调用
/// {@link #drainEvents()} 以单步 handoff 语义提取事件，并完成后续分发。
/// 这里的“原子性”仅描述聚合根单线程使用模型下的读取并清空语义，不提供并发安全承诺。
///
/// @param <T>  聚合根本类型（self type，编译期锁定聚合根身份）
/// @param <ID> 标识符类型
///
public abstract class BaseAggregateRoot<T extends AggregateRoot<T, ID>, ID extends Identifier>
        implements AggregateRoot<T, ID>, EventRecordable {

    /// 标识符。
    private ID id;

    /// 领域事件列表。
    /// <p>
    /// transient: 不参与序列化，避免持久化或缓存污染事件状态。
    /// <p>
    /// 线程安全契约：聚合根非线程安全，events 列表使用 {@link ArrayList} 实现。
    /// 业务代码禁止跨线程共享同一个聚合根实例，包括异步任务（如 JobRunr）、MQ 监听器等场景。
    /// 跨线程传递聚合状态应通过不可变快照或重新加载聚合实例实现。
    protected transient List<DomainEvent> events;

    public BaseAggregateRoot(ID id) {
        this.id = id;
    }

    @Override
    public ID getId() {
        return id;
    }

    /// 重新指定标识符。
    /// <p>
    /// 该方法只面向子类和持久化转换场景开放，业务代码应优先通过构造函数确定聚合标识。
    ///
    /// @param id 标识符
    protected void identify(ID id) {
        this.id = id;
    }

    /// 记录领域事件。
    ///
    /// 聚合根负责记录自身边界内已经发生的领域事件，供应用层在用例边界统一提取和分发。
    ///
    /// @param event 领域事件
    protected void recordEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event must not be null.");
        }
        if (events == null) {
            events = new ArrayList<>();
        }
        events.add(event);
    }

    @Override
    public List<DomainEvent> drainEvents() {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        List<DomainEvent> drainedEvents = List.copyOf(events);
        events.clear();
        return drainedEvents;
    }
}
