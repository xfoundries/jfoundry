package org.jfoundry.application.outbox;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOutboxDispatchServiceTest {

    private final RecordingOutboxMessageStore store = new RecordingOutboxMessageStore();
    private final BackoffStrategy backoff = attempts -> Duration.ofSeconds(attempts);

    @Test
    void marksClaimedMessagesPublishedWhenSendSucceeds() {
        store.messages = List.of(message("evt-1"));
        DefaultOutboxDispatchService service = new DefaultOutboxDispatchService(
                store, (topic, key, payload) -> SendResult.ok(), 3, backoff, "pod-1");

        service.dispatch(10);

        assertThat(store.claimBatchSize).isEqualTo(10);
        assertThat(store.claimerId).isEqualTo("pod-1");
        assertThat(store.published).containsExactly("evt-1");
        assertThat(store.failed).isEmpty();
    }

    @Test
    void marksMessageFailedWhenSendReturnsFailure() {
        store.messages = List.of(message("evt-1"));
        MessageSender sender = (topic, key, payload) -> SendResult.fail("broker unavailable");
        DefaultOutboxDispatchService service = new DefaultOutboxDispatchService(store, sender, 5, backoff, "pod-1");

        service.dispatch(1);

        assertThat(store.failed).containsExactly("evt-1:broker unavailable:5");
        assertThat(store.published).isEmpty();
    }

    @Test
    void marksMessageFailedWhenSenderThrowsAndContinues() {
        store.messages = List.of(message("evt-1"), message("evt-2"));
        MessageSender sender = (topic, key, payload) -> {
            if ("key-1".equals(key)) {
                throw new IllegalStateException("boom");
            }
            return SendResult.ok();
        };
        DefaultOutboxDispatchService service = new DefaultOutboxDispatchService(store, sender, 2, backoff, "pod-1");

        service.dispatch(2);

        assertThat(store.failed).containsExactly("evt-1:boom:2");
        assertThat(store.published).containsExactly("evt-2");
    }

    private OutboxMessage message(String eventId) {
        String key = eventId.replace("evt-", "key-");
        return OutboxMessage.newPending(eventId, "topic", key, "type", "{}", Instant.now());
    }

    private static class RecordingOutboxMessageStore implements OutboxMessageStore {
        private List<OutboxMessage> messages = List.of();
        private int claimBatchSize;
        private String claimerId;
        private final java.util.List<String> published = new java.util.ArrayList<>();
        private final java.util.List<String> failed = new java.util.ArrayList<>();

        @Override
        public void append(OutboxMessage message) {
        }

        @Override
        public List<OutboxMessage> findDispatchable(int limit, Instant now) {
            return messages.subList(0, Math.min(limit, messages.size()));
        }

        @Override
        public List<OutboxMessage> claimDispatchable(int limit, String claimerId) {
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
        public int deleteByStatusAndOccurredAtBefore(OutboxMessageStatus status, Instant cutoff, int batchSize) {
            return 0;
        }
    }
}
