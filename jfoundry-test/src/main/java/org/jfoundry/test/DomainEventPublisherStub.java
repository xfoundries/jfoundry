package org.jfoundry.test;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jmolecules.event.types.DomainEvent;

/// Spring 发布器的测试替身。
/// <p>
/// 不依赖 Spring 容器和事务同步，立即接收并转发给 DomainEventCapture。
public class DomainEventPublisherStub implements DomainEventPublisher {

    private final DomainEventCapture capture;

    public DomainEventPublisherStub(DomainEventCapture capture) {
        this.capture = capture;
    }

    @Override
    public void publish(DomainEvent... events) {
        if (events == null) {
            throw new IllegalArgumentException("Domain events must not be null.");
        }
        capture.captureAll(events);
    }
}
