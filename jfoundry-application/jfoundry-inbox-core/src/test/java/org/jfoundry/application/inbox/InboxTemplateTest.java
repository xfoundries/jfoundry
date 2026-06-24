package org.jfoundry.application.inbox;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboxTemplateTest {

    @Test
    void skipsAlreadyProcessedMessage() {
        RecordingInboxRepository repository = new RecordingInboxRepository();
        repository.processed = true;
        InboxTemplate template = new InboxTemplate(repository);
        AtomicBoolean called = new AtomicBoolean(false);

        boolean executed = template.executeOnce("evt-1", "projection", () -> called.set(true));

        assertThat(executed).isFalse();
        assertThat(called).isFalse();
    }

    @Test
    void recordsProcessedAfterSuccessfulHandler() {
        RecordingInboxRepository repository = new RecordingInboxRepository();
        InboxTemplate template = new InboxTemplate(repository);

        boolean executed = template.executeOnce("evt-1", "projection", () -> {});

        assertThat(executed).isTrue();
        assertThat(repository.recordedMessageId).isEqualTo("evt-1");
        assertThat(repository.recordedConsumerName).isEqualTo("projection");
    }

    @Test
    void doesNotRecordProcessedWhenHandlerFails() {
        RecordingInboxRepository repository = new RecordingInboxRepository();
        InboxTemplate template = new InboxTemplate(repository);

        assertThatThrownBy(() -> template.executeOnce("evt-1", "projection", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.recordedMessageId).isNull();
    }

    @Test
    void rejectsBlankMessageId() {
        InboxTemplate template = new InboxTemplate(new RecordingInboxRepository());

        assertThatThrownBy(() -> template.executeOnce(" ", "projection", () -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    static class RecordingInboxRepository implements InboxRepository {
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
