package org.jfoundry.infrastructure.outbox.jobrunr.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.OutboxEntry;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobRunrOutboxDispatcherTest {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    private OutboxRepository outboxRepository;
    private MessageSender messageSender;
    private BackoffStrategy backoff;
    private JobRunrOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        messageSender = mock(MessageSender.class);
        backoff = failedAttempts -> Duration.ofSeconds(1);
        dispatcher = new JobRunrOutboxDispatcher(outboxRepository, messageSender, BATCH_SIZE, MAX_RETRIES, backoff);
    }

    @Test
    void dispatchMarksPublishedOnSuccess() {
        OutboxEntry entry = entry("evt-1");
        when(outboxRepository.claimDispatchable(anyInt(), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenReturn(SendResult.ok());

        dispatcher.dispatch(BATCH_SIZE);

        verify(outboxRepository).markAsPublished("evt-1");
        verify(outboxRepository, never()).markAsFailed(any(), any(), anyInt(), any());
    }

    @Test
    void dispatchMarksFailedOnSenderException() {
        OutboxEntry entry = entry("evt-1");
        when(outboxRepository.claimDispatchable(anyInt(), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenThrow(new RuntimeException("kafka unavailable"));

        dispatcher.dispatch(BATCH_SIZE);

        verify(outboxRepository).markAsFailed(eq("evt-1"), contains("kafka unavailable"), eq(MAX_RETRIES), same(backoff));
    }

    @Test
    void dispatchMarksFailedOnSendFailureResult() {
        OutboxEntry entry = entry("evt-1");
        when(outboxRepository.claimDispatchable(anyInt(), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenReturn(SendResult.fail("conn refused"));

        dispatcher.dispatch(BATCH_SIZE);

        verify(outboxRepository).markAsFailed(eq("evt-1"), eq("conn refused"), eq(MAX_RETRIES), same(backoff));
    }

    @Test
    void recurringDispatchUsesConfiguredBatchSize() {
        when(outboxRepository.claimDispatchable(anyInt(), any())).thenReturn(List.of());

        dispatcher.recurringDispatch();

        verify(outboxRepository).claimDispatchable(eq(BATCH_SIZE), any());
    }

    @Test
    void dispatchPassesTopicAndPayloadToSender() {
        OutboxEntry entry = entry("evt-1");
        when(outboxRepository.claimDispatchable(anyInt(), any())).thenReturn(List.of(entry));
        when(messageSender.send(any(), any(), any())).thenReturn(SendResult.ok());

        dispatcher.dispatch(BATCH_SIZE);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageSender).send(topicCaptor.capture(), eq("key-A"), eq("payload-evt-1"));
        assertThat(topicCaptor.getValue()).isEqualTo("env.created");
    }

    private OutboxEntry entry(String eventId) {
        return OutboxEntry.newPending(
                eventId, "env.created", "key-A", "com.example.Foo", "payload-" + eventId, Instant.now());
    }
}
