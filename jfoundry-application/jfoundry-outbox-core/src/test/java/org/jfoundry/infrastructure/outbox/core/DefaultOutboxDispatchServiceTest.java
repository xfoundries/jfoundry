package org.jfoundry.infrastructure.outbox.core;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOutboxDispatchServiceTest {

    private final RecordingRepository repository = new RecordingRepository();
    private final BackoffStrategy backoff = attempts -> Duration.ofSeconds(attempts);

    @Test
    void marksClaimedEntriesPublishedWhenSendSucceeds() {
        repository.entries = List.of(entry("evt-1"));
        DefaultOutboxDispatchService service = new DefaultOutboxDispatchService(
                repository, (topic, key, payload) -> SendResult.ok(), 3, backoff, "pod-1");

        service.dispatch(10);

        assertThat(repository.claimBatchSize).isEqualTo(10);
        assertThat(repository.claimerId).isEqualTo("pod-1");
        assertThat(repository.published).containsExactly("evt-1");
        assertThat(repository.failed).isEmpty();
    }

    @Test
    void marksEntryFailedWhenSendReturnsFailure() {
        repository.entries = List.of(entry("evt-1"));
        MessageSender sender = (topic, key, payload) -> SendResult.fail("broker unavailable");
        DefaultOutboxDispatchService service = new DefaultOutboxDispatchService(repository, sender, 5, backoff, "pod-1");

        service.dispatch(1);

        assertThat(repository.failed).containsExactly("evt-1:broker unavailable:5");
        assertThat(repository.published).isEmpty();
    }

    @Test
    void marksEntryFailedWhenSenderThrowsAndContinues() {
        repository.entries = List.of(entry("evt-1"), entry("evt-2"));
        MessageSender sender = (topic, key, payload) -> {
            if ("key-1".equals(key)) {
                throw new IllegalStateException("boom");
            }
            return SendResult.ok();
        };
        DefaultOutboxDispatchService service = new DefaultOutboxDispatchService(repository, sender, 2, backoff, "pod-1");

        service.dispatch(2);

        assertThat(repository.failed).containsExactly("evt-1:boom:2");
        assertThat(repository.published).containsExactly("evt-2");
    }

    private OutboxEntry entry(String eventId) {
        String key = eventId.replace("evt-", "key-");
        return OutboxEntry.newPending(eventId, "topic", key, "type", "{}", Instant.now());
    }

    private static class RecordingRepository implements OutboxRepository {
        private List<OutboxEntry> entries = List.of();
        private int claimBatchSize;
        private String claimerId;
        private final java.util.List<String> published = new java.util.ArrayList<>();
        private final java.util.List<String> failed = new java.util.ArrayList<>();

        @Override
        public void append(OutboxEntry entry) {
        }

        @Override
        public List<OutboxEntry> findDispatchable(int limit, Instant now) {
            return entries.subList(0, Math.min(limit, entries.size()));
        }

        @Override
        public List<OutboxEntry> claimDispatchable(int limit, String claimerId) {
            this.claimBatchSize = limit;
            this.claimerId = claimerId;
            return findDispatchable(limit, Instant.now());
        }

        @Override
        public void markAsPublished(String eventId) {
            published.add(eventId);
        }

        @Override
        public void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff) {
            failed.add(eventId + ":" + errorMessage + ":" + maxRetries);
        }

        @Override
        public void reactivate(String eventId) {
        }

        @Override
        public int recoverStuckDispatching(Instant cutoff) {
            return 0;
        }

        @Override
        public int deleteByStatusAndOccurredAtBefore(OutboxStatus status, Instant cutoff, int batchSize) {
            return 0;
        }
    }
}
