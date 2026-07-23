package org.jfoundry.application.outbox;

import org.jmolecules.event.types.DomainEvent;

import java.util.List;

/**
 * Application-layer contract for recording domain events into the transactional outbox.
 */
public interface DomainEventOutboxRecorder {

    void record(List<? extends DomainEvent> events);
}
