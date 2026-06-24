package org.jfoundry.infrastructure.messaging.spring.publisher;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.application.messaging.externalization.DomainEventSink;
import org.jmolecules.event.types.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.List;

/// 基于 Spring 的领域事件发布器实现。
/// <p>
/// 发布流程：
/// <ol>
///   <li>同步转发给所有 DomainEventSink（事务内，与业务数据原子提交）</li>
///   <li>注册 afterCommit 回调，事务提交后通过 ApplicationEventPublisher 发布给本地监听器</li>
/// </ol>
/// <p>
/// 若无事务上下文，立即同步发布给 Sinks 和本地监听器。
/// <p>
/// 不再标注 {@code @Component}：由 {@code DomainEventPublisherAutoConfiguration} 注册为 bean。
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final List<DomainEventSink> sinks;

    /// 主构造器：注入 sinks（可能为空 list）。
    public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher,
                                      List<DomainEventSink> sinks) {
        this.eventPublisher = eventPublisher;
        this.sinks = sinks != null ? sinks : List.of();
    }

    /// 兼容构造器：无 Sink 场景（如纯领域测试）。
    public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this(eventPublisher, List.of());
    }

    @Override
    public void publish(DomainEvent... events) {
        if (events == null) {
            throw new IllegalArgumentException("Domain events must not be null.");
        }
        List<DomainEvent> eventList = Arrays.stream(events)
                .map(this::requireEvent)
                .toList();
        if (eventList.isEmpty()) {
            return;
        }

        // 1. 同步转发给 Sinks（事务内）
        for (DomainEvent event : eventList) {
            for (DomainEventSink sink : sinks) {
                sink.handle(event);
            }
        }

        // 2. 注册 afterCommit 回调发布给本地监听器
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventList.forEach(eventPublisher::publishEvent);
                }
            });
            return;
        }
        eventList.forEach(eventPublisher::publishEvent);
    }

    private DomainEvent requireEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event must not be null.");
        }
        return event;
    }
}
