package org.jfoundry.domain.entity.agg;

import org.jfoundry.domain.event.AbstractDomainEvent;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseAggregateRootTest {

    @Test
    void aggregateRootStoresFrameworkIndependentDomainEvents() {
        TestAggregateRoot aggregateRoot = new TestAggregateRoot(new TestAggregateId("root-1"));
        TestDomainEvent event = new TestDomainEvent(new TestAggregateId("root-1"));

        aggregateRoot.raise(event);

        assertEquals(1, aggregateRoot.getEvents().size());
        assertEquals(event, aggregateRoot.getEvents().getFirst());
    }

    @Test
    void clearEventsRemovesPendingDomainEvents() {
        TestAggregateRoot aggregateRoot = new TestAggregateRoot(new TestAggregateId("root-1"));
        aggregateRoot.raise(new TestDomainEvent(new TestAggregateId("root-1")));

        aggregateRoot.clearEvents();

        assertTrue(aggregateRoot.getEvents().isEmpty());
    }

    @Test
    void recordDomainEventRejectsNullEvent() {
        TestAggregateRoot aggregateRoot = new TestAggregateRoot(new TestAggregateId("root-1"));

        assertThrows(IllegalArgumentException.class, () -> aggregateRoot.raise(null));
        assertTrue(aggregateRoot.getEvents().isEmpty());
    }

    record TestAggregateId(String value) implements Identifier {
    }

    private static class TestAggregateRoot extends BaseAggregateRoot<TestAggregateRoot, TestAggregateId> {
        private TestAggregateRoot(TestAggregateId id) {
            super(id);
        }

        private void raise(DomainEvent event) {
            recordEvent(event);
        }
    }

    private static class TestDomainEvent extends AbstractDomainEvent {
        private final TestAggregateId aggregateId;

        TestDomainEvent(TestAggregateId aggregateId) {
            super();
            this.aggregateId = aggregateId;
        }

        public TestAggregateId getAggregateId() {
            return aggregateId;
        }
    }
}
