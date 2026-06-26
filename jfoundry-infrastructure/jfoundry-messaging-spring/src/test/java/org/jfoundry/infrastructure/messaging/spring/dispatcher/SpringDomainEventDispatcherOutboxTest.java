package org.jfoundry.infrastructure.messaging.spring.dispatcher;

import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringDomainEventDispatcherOutboxTest {

    @Test
    void dispatchRecordsOutboxBeforePublishingSpringEvents() {
        RecordingOutboxRecorder outboxRecorder = new RecordingOutboxRecorder();
        RecordingApplicationEventPublisher publisher = new RecordingApplicationEventPublisher();
        SpringDomainEventDispatcher dispatcher = new SpringDomainEventDispatcher(outboxRecorder, publisher);
        TestDomainEvent event = new TestDomainEvent();

        dispatcher.dispatch(List.of(event));

        assertThat(outboxRecorder.recordedEvents).containsExactly(event);
        assertThat(publisher.publishedEvents).containsExactly(event);
        assertThat(outboxRecorder.recordCallIndex).isLessThan(publisher.publishCallIndex);
    }

    @Test
    void outboxFailurePropagatesAndPreventsSpringPublication() {
        RuntimeException failure = new RuntimeException("outbox boom");
        ThrowingOutboxRecorder outboxRecorder = new ThrowingOutboxRecorder(failure);
        RecordingApplicationEventPublisher publisher = new RecordingApplicationEventPublisher();
        SpringDomainEventDispatcher dispatcher = new SpringDomainEventDispatcher(outboxRecorder, publisher);
        TestDomainEvent event = new TestDomainEvent();

        assertThatThrownBy(() -> dispatcher.dispatch(List.of(event)))
                .isSameAs(failure);

        assertThat(publisher.publishedEvents).isEmpty();
    }

    private static final class RecordingOutboxRecorder implements DomainEventOutboxRecorder {

        private final List<DomainEvent> recordedEvents = new ArrayList<>();
        private int recordCallIndex;

        @Override
        public void record(List<? extends DomainEvent> events) {
            recordCallIndex = CallSequence.next();
            recordedEvents.addAll(events);
        }
    }

    private static final class ThrowingOutboxRecorder implements DomainEventOutboxRecorder {

        private final RuntimeException failure;

        private ThrowingOutboxRecorder(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void record(List<? extends DomainEvent> events) {
            throw failure;
        }
    }

    private static final class RecordingApplicationEventPublisher implements ApplicationEventPublisher {

        private final List<Object> publishedEvents = new ArrayList<>();
        private int publishCallIndex;

        @Override
        public void publishEvent(Object event) {
            publishCallIndex = CallSequence.next();
            publishedEvents.add(event);
        }
    }

    private static final class CallSequence {

        private static int index;

        private static int next() {
            return ++index;
        }
    }

    private static final class TestDomainEvent implements DomainEvent {
    }
}
