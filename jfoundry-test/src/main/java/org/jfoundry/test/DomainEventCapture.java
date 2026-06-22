package org.jfoundry.test;

import org.jmolecules.event.types.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/// 捕获聚合事件的测试工具。
/// <p>
/// 与 DomainEventPublisherStub 配合使用，断言事件是否被移交和发布。
public class DomainEventCapture {

    private final List<DomainEvent> captured = new ArrayList<>();

    public void capture(DomainEvent event) {
        captured.add(event);
    }

    public void captureAll(DomainEvent... events) {
        for (DomainEvent event : events) {
            captured.add(event);
        }
    }

    public List<DomainEvent> drained() {
        List<DomainEvent> snapshot = List.copyOf(captured);
        captured.clear();
        return snapshot;
    }

    public List<DomainEvent> snapshot() {
        return List.copyOf(captured);
    }

    public int size() {
        return captured.size();
    }
}
