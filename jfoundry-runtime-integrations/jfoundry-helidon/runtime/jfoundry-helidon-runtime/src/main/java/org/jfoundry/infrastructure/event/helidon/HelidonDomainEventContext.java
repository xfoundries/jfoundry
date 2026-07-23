package org.jfoundry.infrastructure.event.helidon;

import jakarta.enterprise.context.ApplicationScoped;
import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.domain.event.EventRecordable;

import java.util.Objects;

/// Default CDI adapter that registers aggregates in the active Helidon event scope.
@ApplicationScoped
public final class HelidonDomainEventContext implements DomainEventContext {

    private final HelidonDomainEventScope scope;

    public HelidonDomainEventContext(HelidonDomainEventScope scope) {
        this.scope = Objects.requireNonNull(scope, "HelidonDomainEventScope must not be null");
    }

    @Override
    public void register(EventRecordable aggregate) {
        scope.register(aggregate);
    }
}
