package org.jfoundry.infrastructure.outbox.helidon.externalization;

import jakarta.enterprise.inject.Instance;
import org.jfoundry.application.event.externalization.ExternalizationRuleResolver;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class HelidonDomainEventOutboxRecorderTest {

    @Test
    void ignoresUnmarkedEventsWithoutResolvingTheOutboxStoreOrSerializer() {
        HelidonDomainEventOutboxRecorder recorder = new HelidonDomainEventOutboxRecorder(
                unavailable(OutboxMessageStore.class), unavailable(org.jfoundry.application.messaging.PayloadSerializer.class),
                new ExternalizationRuleResolver(), new org.jfoundry.application.event.externalization.AggregateRoutingResolver());

        assertThatCode(() -> recorder.record(List.of(new InternalEvent()))).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static <T> Instance<T> unavailable(Class<T> type) {
        return (Instance<T>) Proxy.newProxyInstance(
                HelidonDomainEventOutboxRecorderTest.class.getClassLoader(), new Class<?>[]{Instance.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError(type.getSimpleName() + " must not be resolved for an unmarked event");
                });
    }

    private record InternalEvent() implements DomainEvent {
    }
}
