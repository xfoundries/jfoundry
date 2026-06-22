package org.jfoundry.infrastructure.messaging.spring.publisher;

import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringDomainEventPublisherTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishImmediatelyWhenNoTransactionSynchronizationActive() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        TestDomainEvent event = new TestDomainEvent("order-1");

        publisher.publish(event);

        assertEquals(List.of(event), applicationEventPublisher.publishedEvents());
    }

    @Test
    void deferPublishUntilTransactionCommitWhenSynchronizationActive() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        TestDomainEvent event = new TestDomainEvent("order-1");

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(event);

        assertTrue(applicationEventPublisher.publishedEvents().isEmpty());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertEquals(List.of(event), applicationEventPublisher.publishedEvents());
    }

    @Test
    void doNotPublishWhenTransactionRollsBack() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        TestDomainEvent event = new TestDomainEvent("order-1");

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(applicationEventPublisher.publishedEvents().isEmpty());
    }

    @Test
    void publishBatchImmediatelyWhenNoTransactionSynchronizationActive() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        TestDomainEvent first = new TestDomainEvent("order-1");
        TestDomainEvent second = new TestDomainEvent("order-2");

        publisher.publish(first, second);

        assertEquals(List.of(first, second), applicationEventPublisher.publishedEvents());
    }

    @Test
    void publishRegistersSingleTransactionSynchronizationForBatch() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        TestDomainEvent first = new TestDomainEvent("order-1");
        TestDomainEvent second = new TestDomainEvent("order-2");

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(first, second);

        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        assertTrue(applicationEventPublisher.publishedEvents().isEmpty());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertEquals(List.of(first, second), applicationEventPublisher.publishedEvents());
    }

    @Test
    void publishDoesNothingForEmptyArguments() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);

        TransactionSynchronizationManager.initSynchronization();
        publisher.publish();

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        assertTrue(applicationEventPublisher.publishedEvents().isEmpty());
    }

    @Test
    void rejectNullEventArray() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publisher.publish((DomainEvent[]) null));

        assertEquals("Domain events must not be null.", exception.getMessage());
    }

    @Test
    void rejectNullEventElement() {
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        TestDomainEvent event = new TestDomainEvent("order-1");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(event, null));

        assertEquals("Domain event must not be null.", exception.getMessage());
        assertTrue(applicationEventPublisher.publishedEvents().isEmpty());
    }

    private record TestDomainEvent(String aggregateId) implements DomainEvent {
    }

    private static class RecordingApplicationEventPublisher implements ApplicationEventPublisher {
        private final List<Object> publishedEvents = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            publishedEvents.add(event);
        }

        private List<Object> publishedEvents() {
            return publishedEvents;
        }
    }
}
