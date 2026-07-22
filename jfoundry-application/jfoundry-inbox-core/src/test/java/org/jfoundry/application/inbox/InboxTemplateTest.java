package org.jfoundry.application.inbox;

import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionPropagation;
import org.jfoundry.application.transaction.TransactionRunner;
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

    @Test
    void processesAndMarksMessageInOneTransactionAfterAnIndependentClaim() {
        RecordingTransactionRunner transactions = new RecordingTransactionRunner();
        TransactionAwareInboxMessageStore store = new TransactionAwareInboxMessageStore(transactions);
        InboxTemplate template = new InboxTemplate(store, transactions);

        boolean executed = template.executeOnce("evt-1", "projection", () ->
                assertThat(transactions.inTransaction).isTrue());

        assertThat(executed).isTrue();
        assertThat(store.claimedInTransaction).isTrue();
        assertThat(store.processedInTransaction).isTrue();
        assertThat(transactions.options)
                .extracting(TransactionOptions::propagation)
                .containsExactly(TransactionPropagation.REQUIRES_NEW, TransactionPropagation.REQUIRES_NEW);
        assertThat(transactions.options)
                .allSatisfy(options -> assertThat(options.name()).isEmpty());
    }

    @Test
    void recordsHandlerFailureInANewTransaction() {
        RecordingTransactionRunner transactions = new RecordingTransactionRunner();
        TransactionAwareInboxMessageStore store = new TransactionAwareInboxMessageStore(transactions);
        InboxTemplate template = new InboxTemplate(store, transactions);

        assertThatThrownBy(() -> template.executeOnce("evt-1", "projection", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.failedInTransaction).isTrue();
        assertThat(store.failure).isEqualTo("boom");
        assertThat(transactions.options)
                .extracting(TransactionOptions::propagation)
                .containsExactly(
                        TransactionPropagation.REQUIRES_NEW,
                        TransactionPropagation.REQUIRES_NEW,
                        TransactionPropagation.REQUIRES_NEW);
        assertThat(transactions.options)
                .allSatisfy(options -> assertThat(options.name()).isEmpty());
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

    private static final class TransactionAwareInboxMessageStore implements InboxMessageStore {
        private final RecordingTransactionRunner transactions;
        private boolean claimedInTransaction;
        private boolean processedInTransaction;
        private boolean failedInTransaction;
        private String failure;

        private TransactionAwareInboxMessageStore(RecordingTransactionRunner transactions) {
            this.transactions = transactions;
        }

        @Override
        public boolean isProcessed(String messageId, String consumerName) {
            return false;
        }

        @Override
        public boolean tryStartProcessing(String messageId, String consumerName) {
            claimedInTransaction = transactions.inTransaction;
            return true;
        }

        @Override
        public void markProcessed(String messageId, String consumerName) {
            processedInTransaction = transactions.inTransaction;
        }

        @Override
        public void markFailed(String messageId, String consumerName, String errorMessage) {
            failedInTransaction = transactions.inTransaction;
            failure = errorMessage;
        }
    }

    private static final class RecordingTransactionRunner implements TransactionRunner {
        private final java.util.List<TransactionOptions> options = new java.util.ArrayList<>();
        private boolean inTransaction;

        @Override
        public <T> T call(TransactionOptions options, TransactionCallback<T> callback) throws Exception {
            this.options.add(options);
            inTransaction = true;
            try {
                return callback.execute();
            } finally {
                inTransaction = false;
            }
        }
    }
}
