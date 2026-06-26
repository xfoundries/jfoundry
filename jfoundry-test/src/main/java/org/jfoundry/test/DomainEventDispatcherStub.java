package org.jfoundry.test;

import org.jfoundry.application.event.DomainEventDispatcher;
import org.jmolecules.event.types.DomainEvent;

import java.util.List;

/// DomainEventDispatcher 的测试替身。
/// <p>
/// 不依赖 Spring 容器和事务同步，立即接收并转发给 DomainEventCapture。
public class DomainEventDispatcherStub implements DomainEventDispatcher {

    private final DomainEventCapture capture;

    public DomainEventDispatcherStub(DomainEventCapture capture) {
        this.capture = capture;
    }

    @Override
    public void dispatch(List<? extends DomainEvent> events) {
        if (events == null) {
            throw new IllegalArgumentException("Domain events must not be null.");
        }
        capture.captureAll(events);
    }
}
