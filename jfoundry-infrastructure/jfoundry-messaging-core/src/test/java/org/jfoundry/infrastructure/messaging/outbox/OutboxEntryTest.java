package org.jfoundry.infrastructure.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEntryTest {

    private final BackoffStrategy fixedBackoff = failedAttempts -> Duration.ofSeconds(10);

    @Test
    void newPendingInitializesFields() {
        Instant before = Instant.now();
        OutboxEntry entry = OutboxEntry.newPending(
                "evt-1", "topic-1", "key-1", "com.example.FooEvent", "{}", Instant.parse("2026-06-18T10:00:00Z"));

        assertThat(entry.getEventId()).isEqualTo("evt-1");
        assertThat(entry.getTopic()).isEqualTo("topic-1");
        assertThat(entry.getPayloadKey()).isEqualTo("key-1");
        assertThat(entry.getPayloadType()).isEqualTo("com.example.FooEvent");
        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entry.getRetryCount()).isZero();
        assertThat(entry.getNextRetryAt()).isNull();
        assertThat(entry.getLastAttemptAt()).isNull();
        assertThat(entry.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));
    }

    @Test
    void markPublishedTransitionsToPublished() {
        OutboxEntry entry = newPending();
        entry.markPublished();

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    void markFailedUnderMaxRetriesTransitionsToFailedAndSetsNextRetryAt() {
        OutboxEntry entry = newPending();
        int maxRetries = 5;

        entry.markFailed("boom", maxRetries, fixedBackoff);

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(entry.getRetryCount()).isEqualTo(1);
        assertThat(entry.getErrorMessage()).isEqualTo("boom");
        assertThat(entry.getLastAttemptAt()).isNotNull();
        assertThat(entry.getNextRetryAt()).isNotNull();
    }

    @Test
    void markFailedUsesBackoffBasedOnRetryCountBeforeIncrement() {
        OutboxEntry entry = newPending();
        int[] captured = new int[1];
        BackoffStrategy capturing = failedAttempts -> {
            captured[0] = failedAttempts;
            return Duration.ofSeconds(failedAttempts + 1);
        };

        entry.markFailed("err", 5, capturing);

        // First failure: retryCountBefore = 0, so backoff receives 0
        assertThat(captured[0]).isZero();
    }

    @Test
    void markFailedAtMaxRetriesTransitionsToDeadLettered() {
        OutboxEntry entry = newPending();
        int maxRetries = 1;

        entry.markFailed("first failure", maxRetries, fixedBackoff);
        // retryCount is now 1, equals maxRetries → DEAD_LETTERED
        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTERED);
        assertThat(entry.getNextRetryAt()).isNull();
    }

    @Test
    void reactivateResetsDeadLeteredToPending() {
        OutboxEntry entry = newPending();
        entry.markFailed("err", 1, fixedBackoff);
        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTERED);

        entry.reactivate();

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entry.getRetryCount()).isZero();
        assertThat(entry.getNextRetryAt()).isNotNull();
        assertThat(entry.getErrorMessage()).isNull();
    }

    @Test
    void reactivateRejectsNonDeadLettered() {
        OutboxEntry entry = newPending();

        assertThatThrownBy(entry::reactivate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEAD_LETTERED");
    }

    private OutboxEntry newPending() {
        return OutboxEntry.newPending(
                "evt-1", "topic-1", null, "com.example.Foo", "{}", Instant.parse("2026-06-18T10:00:00Z"));
    }
}
