package org.jfoundry.infrastructure.messaging.spring.dispatcher;

import org.jfoundry.application.event.DomainEventDispatcher;
import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jmolecules.event.types.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring-backed application-layer domain event dispatcher and orchestration point.
 *
 * <p>Dispatch flow:
 * <ol>
 *   <li>Record matching events into the transactional outbox</li>
 *   <li>Publish the same batch through Spring's application event publisher</li>
 * </ol>
 */
public class SpringDomainEventDispatcher implements DomainEventDispatcher {

    private final DomainEventOutboxRecorder outboxRecorder;
    private final ApplicationEventPublisher eventPublisher;

    public SpringDomainEventDispatcher(DomainEventOutboxRecorder outboxRecorder,
                                       ApplicationEventPublisher eventPublisher) {
        this.outboxRecorder = outboxRecorder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void dispatch(List<? extends DomainEvent> events) {
        if (events == null) {
            throw new IllegalArgumentException("Domain events must not be null.");
        }

        if (events.isEmpty()) {
            return;
        }

        List<DomainEvent> eventBatch = new ArrayList<>(events.size());
        for (DomainEvent event : events) {
            eventBatch.add(requireEvent(event));
        }

        if (outboxRecorder != null) {
            outboxRecorder.record(eventBatch);
        }
        publishSpringEvents(eventBatch);
    }

    private DomainEvent requireEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event must not be null.");
        }
        return event;
    }

    private void publishSpringEvents(List<DomainEvent> events) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    events.forEach(eventPublisher::publishEvent);
                }
            });
            return;
        }
        events.forEach(eventPublisher::publishEvent);
    }
}
