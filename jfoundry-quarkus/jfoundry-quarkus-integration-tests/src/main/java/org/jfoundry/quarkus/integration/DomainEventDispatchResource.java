package org.jfoundry.quarkus.integration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jfoundry.application.ApplicationService;
import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.application.event.DomainEventDispatcher;
import org.jfoundry.domain.event.EventRecordable;
import org.jmolecules.event.types.DomainEvent;

import java.util.ArrayList;
import java.util.List;

@Path("/jfoundry/domain-events")
@ApplicationScoped
public class DomainEventDispatchResource {

    private final DomainEventApplicationService applicationService;
    private final RecordingDomainEventDispatcher dispatcher;

    @Inject
    public DomainEventDispatchResource(
            DomainEventApplicationService applicationService,
            RecordingDomainEventDispatcher dispatcher) {
        this.applicationService = applicationService;
        this.dispatcher = dispatcher;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String dispatchNestedEvents() {
        dispatcher.reset();
        applicationService.handle();
        return dispatcher.dispatchedAggregateIds().toString();
    }

    @GET
    @Path("/failure")
    @Produces(MediaType.TEXT_PLAIN)
    public String discardEventsWhenApplicationServiceFails() {
        dispatcher.reset();
        try {
            applicationService.handleAndFail();
        } catch (IllegalStateException exception) {
            return dispatcher.dispatchedAggregateIds().toString();
        }
        throw new IllegalStateException("Expected application service to fail");
    }

    @ApplicationScoped
    @ApplicationService
    static class DomainEventApplicationService {

        private final DomainEventContext domainEventContext;
        private final NestedDomainEventApplicationService nestedApplicationService;

        @Inject
        DomainEventApplicationService(
                DomainEventContext domainEventContext,
                NestedDomainEventApplicationService nestedApplicationService) {
            this.domainEventContext = domainEventContext;
            this.nestedApplicationService = nestedApplicationService;
        }

        void handle() {
            domainEventContext.register(new EventAggregate("outer"));
            nestedApplicationService.handle();
        }

        void handleAndFail() {
            domainEventContext.register(new EventAggregate("failed"));
            throw new IllegalStateException("failure");
        }
    }

    @ApplicationScoped
    @ApplicationService
    static class NestedDomainEventApplicationService {

        private final DomainEventContext domainEventContext;

        @Inject
        NestedDomainEventApplicationService(DomainEventContext domainEventContext) {
            this.domainEventContext = domainEventContext;
        }

        void handle() {
            domainEventContext.register(new EventAggregate("inner"));
        }
    }

    @ApplicationScoped
    static class RecordingDomainEventDispatcher implements DomainEventDispatcher {

        private final List<String> aggregateIds = new ArrayList<>();

        @Override
        public void dispatch(List<? extends DomainEvent> events) {
            events.forEach(event -> aggregateIds.add(((RecordedDomainEvent) event).aggregateId()));
        }

        List<String> dispatchedAggregateIds() {
            return List.copyOf(aggregateIds);
        }

        void reset() {
            aggregateIds.clear();
        }
    }

    private static final class EventAggregate implements EventRecordable {

        private List<DomainEvent> events;

        private EventAggregate(String aggregateId) {
            events = List.of(new RecordedDomainEvent(aggregateId));
        }

        @Override
        public List<DomainEvent> drainEvents() {
            List<DomainEvent> drained = events;
            events = List.of();
            return drained;
        }
    }

    private record RecordedDomainEvent(String aggregateId) implements DomainEvent {
    }
}
