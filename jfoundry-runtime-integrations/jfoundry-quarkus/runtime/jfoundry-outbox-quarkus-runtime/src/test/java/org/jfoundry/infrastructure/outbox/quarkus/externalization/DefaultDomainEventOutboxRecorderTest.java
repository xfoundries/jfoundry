package org.jfoundry.infrastructure.outbox.quarkus.externalization;

import jakarta.enterprise.inject.Instance;
import org.jfoundry.application.event.externalization.AggregateRoutingResolver;
import org.jfoundry.application.event.externalization.ExternalizationRuleResolver;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class DefaultDomainEventOutboxRecorderTest {

    @Test
    void ignoresUnmarkedEventsWithoutResolvingTheOutboxStore() {
        DefaultDomainEventOutboxRecorder recorder = new DefaultDomainEventOutboxRecorder(
                unavailableOutboxStore(),
                event -> {
                    throw new AssertionError("Unmarked event must not be serialized");
                },
                new ExternalizationRuleResolver(),
                new AggregateRoutingResolver());

        assertThatCode(() -> recorder.record(List.of(new InternalDomainEvent())))
                .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static Instance<OutboxMessageStore> unavailableOutboxStore() {
        return (Instance<OutboxMessageStore>) Proxy.newProxyInstance(
                DefaultDomainEventOutboxRecorderTest.class.getClassLoader(),
                new Class<?>[]{Instance.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("Outbox store must not be resolved for an unmarked event");
                });
    }

    private record InternalDomainEvent() implements DomainEvent {
    }
}
