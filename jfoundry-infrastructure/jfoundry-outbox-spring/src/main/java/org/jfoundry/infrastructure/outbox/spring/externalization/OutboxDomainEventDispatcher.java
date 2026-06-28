package org.jfoundry.infrastructure.outbox.spring.externalization;

import org.jfoundry.application.event.DomainEventBatch;
import org.jfoundry.application.event.DomainEventDispatcher;
import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jmolecules.event.types.DomainEvent;

import java.util.List;

/**
 * Records domain events into the transactional outbox.
 */
public class OutboxDomainEventDispatcher implements DomainEventDispatcher {

    private final DomainEventOutboxRecorder outboxRecorder;

    public OutboxDomainEventDispatcher(DomainEventOutboxRecorder outboxRecorder) {
        this.outboxRecorder = outboxRecorder;
    }

    @Override
    public void dispatch(List<? extends DomainEvent> events) {
        List<DomainEvent> eventBatch = DomainEventBatch.copyAndValidate(events);
        if (eventBatch.isEmpty()) {
            return;
        }
        outboxRecorder.record(eventBatch);
    }
}
