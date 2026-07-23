package org.jfoundry.infrastructure.event.helidon;

import jakarta.enterprise.context.ApplicationScoped;
import org.jfoundry.application.event.DefaultDomainEventContext;
import org.jfoundry.domain.event.EventRecordable;
import org.jmolecules.event.types.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/// Thread-bound domain-event scope for a Helidon application-service invocation.
@ApplicationScoped
public class HelidonDomainEventScope {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    <T> T invoke(ScopedOperation<T> operation) throws Exception {
        if (CURRENT.get() != null) {
            return operation.call(false);
        }
        CURRENT.set(new State());
        try {
            return operation.call(true);
        } finally {
            CURRENT.remove();
        }
    }

    void register(EventRecordable aggregate) {
        State state = CURRENT.get();
        if (state != null) {
            state.context.register(aggregate);
        }
    }

    void markFailed() {
        State state = CURRENT.get();
        if (state != null) {
            state.failed = true;
        }
    }

    boolean failed() {
        State state = CURRENT.get();
        return state != null && state.failed;
    }

    List<DomainEvent> drainEvents() {
        State state = CURRENT.get();
        if (state == null) {
            return List.of();
        }
        List<DomainEvent> events = new ArrayList<>();
        for (EventRecordable aggregate : state.context.drainRegistered()) {
            events.addAll(aggregate.drainEvents());
        }
        return List.copyOf(events);
    }

    @FunctionalInterface
    interface ScopedOperation<T> {
        T call(boolean outermost) throws Exception;
    }

    private static final class State {
        private final DefaultDomainEventContext context = new DefaultDomainEventContext();
        private boolean failed;
    }
}
