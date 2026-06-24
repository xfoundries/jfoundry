package org.jfoundry.application.inbox;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboxTemplateTest {

    @Test
    void skipsAlreadyProcessedMessage() {
        RecordingInboxMessageStore store = new RecordingInboxMessageStore();
        store.processed = true;
        InboxTemplate template = new InboxTemplate(store);
        AtomicBoolean called = new AtomicBoolean(false);

        boolean executed = template.executeOnce("evt-1", "projection", () -> called.set(true));

        assertThat(executed).isFalse();
        assertThat(called).isFalse();
    }

    @Test
    void recordsProcessedAfterSuccessfulHandler() {
        RecordingInboxMessageStore store = new RecordingInboxMessageStore();
        InboxTemplate template = new InboxTemplate(store);

        boolean executed = template.executeOnce("evt-1", "projection", () -> {});

        assertThat(executed).isTrue();
        assertThat(store.recordedMessageId).isEqualTo("evt-1");
        assertThat(store.recordedConsumerName).isEqualTo("projection");
    }

    @Test
    void doesNotRecordProcessedWhenHandlerFails() {
        RecordingInboxMessageStore store = new RecordingInboxMessageStore();
        InboxTemplate template = new InboxTemplate(store);

        assertThatThrownBy(() -> template.executeOnce("evt-1", "projection", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.recordedMessageId).isNull();
    }

    @Test
    void rejectsBlankMessageId() {
        InboxTemplate template = new InboxTemplate(new RecordingInboxMessageStore());

        assertThatThrownBy(() -> template.executeOnce(" ", "projection", () -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    static class RecordingInboxMessageStore implements InboxMessageStore {
        boolean processed;
        String recordedMessageId;
        String recordedConsumerName;

        @Override
        public boolean isProcessed(String messageId, String consumerName) {
            return processed;
        }

        @Override
        public void markProcessed(String messageId, String consumerName) {
            this.recordedMessageId = messageId;
            this.recordedConsumerName = consumerName;
            this.processed = true;
        }
    }
}
