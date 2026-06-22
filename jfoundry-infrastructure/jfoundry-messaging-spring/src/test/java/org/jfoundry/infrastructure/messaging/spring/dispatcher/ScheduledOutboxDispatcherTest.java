package org.jfoundry.infrastructure.messaging.spring.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScheduledOutboxDispatcherTest {

    private OutboxRepository repository;
    private MessageSender messageSender;
    private BackoffStrategy backoff;
    private ScheduledOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxRepository.class);
        messageSender = mock(MessageSender.class);
        backoff = failedAttempts -> Duration.ofSeconds(1);
        dispatcher = new ScheduledOutboxDispatcher(repository, messageSender, 5, backoff, 5);
    }

    @Test
    void marksAsPublishedOnSendSuccess() {
        OutboxEntry entry = entry("evt-1");
        when(repository.findDispatchable(eq(5), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenReturn(SendResult.ok());

        dispatcher.dispatch(5);

        verify(repository).markAsPublished("evt-1");
        verify(repository, never()).markAsFailed(any(), any(), anyInt(), any());
    }

    @Test
    void marksAsFailedOnSendFailure() {
        OutboxEntry entry = entry("evt-1");
        when(repository.findDispatchable(eq(5), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenReturn(SendResult.fail("conn refused"));

        dispatcher.dispatch(5);

        verify(repository).markAsFailed(eq("evt-1"), eq("conn refused"), eq(5), same(backoff));
        verify(repository, never()).markAsPublished(any());
    }

    @Test
    void marksAsFailedOnSendException() {
        OutboxEntry entry = entry("evt-1");
        when(repository.findDispatchable(eq(5), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenThrow(new RuntimeException("kafka down"));

        dispatcher.dispatch(5);

        verify(repository).markAsFailed(eq("evt-1"), contains("kafka down"), eq(5), same(backoff));
    }

    @Test
    void singleFailureDoesNotBlockRemainingEntries() {
        OutboxEntry first = entry("evt-1");
        OutboxEntry second = entry("evt-2");
        when(repository.findDispatchable(eq(5), any())).thenReturn(List.of(first, second));
        when(messageSender.send(eq("topic"), any(), eq("payload-evt-1")))
                .thenThrow(new RuntimeException("fail"));
        when(messageSender.send(eq("topic"), any(), eq("payload-evt-2")))
                .thenReturn(SendResult.ok());

        dispatcher.dispatch(5);

        verify(repository).markAsFailed(eq("evt-1"), any(), eq(5), any());
        verify(repository).markAsPublished("evt-2");
    }

    @Test
    void passesPayloadKeyToSender() {
        OutboxEntry entry = entry("evt-1");
        entry.setPayloadKey("key-A");
        when(repository.findDispatchable(eq(5), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenReturn(SendResult.ok());

        dispatcher.dispatch(5);

        verify(messageSender).send("topic", "key-A", "payload-evt-1");
    }

    private OutboxEntry entry(String eventId) {
        return OutboxEntry.newPending(
                eventId, "topic", null, "com.example.Foo", "payload-" + eventId, Instant.now());
    }
}
