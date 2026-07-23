package org.jfoundry.infrastructure.event.helidon;

import org.jfoundry.domain.entity.agg.BaseAggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelidonDomainEventScopeTest {

    @Test
    void drainsRegisteredAggregateEventsOnlyAtTheOutermostScope() throws Exception {
        HelidonDomainEventScope scope = new HelidonDomainEventScope();
        TestAggregate aggregate = new TestAggregate();
        aggregate.record(new TestEvent("confirmed"));

        List<DomainEvent> events = scope.invoke(outermost -> {
            scope.register(aggregate);
            return outermost ? scope.drainEvents() : List.of();
        });

        assertThat(events).extracting(event -> ((TestEvent) event).name()).containsExactly("confirmed");
    }

    private static final class TestAggregate extends BaseAggregateRoot<TestAggregate, TestAggregateId> {

        private TestAggregate() {
            super(new TestAggregateId("order-1"));
        }

        private void record(DomainEvent event) {
            recordEvent(event);
        }
    }

    private record TestAggregateId(String value) implements Identifier {
    }

    private record TestEvent(String name) implements DomainEvent {
    }
}
