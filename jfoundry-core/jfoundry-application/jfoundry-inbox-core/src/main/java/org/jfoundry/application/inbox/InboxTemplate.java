package org.jfoundry.application.inbox;

import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionPropagation;
import org.jfoundry.application.transaction.TransactionRunner;

import java.util.Objects;

public class InboxTemplate {

    private final InboxMessageStore store;
    private final TransactionRunner transactionRunner;

    public InboxTemplate(InboxMessageStore store) {
        this(store, null);
    }

    /// Creates an Inbox template with explicit boundaries for claim, processing, and failure state.
    public InboxTemplate(InboxMessageStore store, TransactionRunner transactionRunner) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.transactionRunner = transactionRunner;
    }

    public boolean executeOnce(String messageId, String consumerName, InboxHandler handler) {
        requireText(messageId, "messageId");
        requireText(consumerName, "consumerName");
        Objects.requireNonNull(handler, "handler must not be null");

        if (transactionRunner == null) {
            return executeWithoutTransaction(messageId, consumerName, handler);
        }

        if (!inNewTransaction(() -> store.tryStartProcessing(messageId, consumerName))) {
            return false;
        }
        try {
            inNewTransaction(() -> {
                handler.handle();
                store.markProcessed(messageId, consumerName);
                return null;
            });
            return true;
        } catch (RuntimeException e) {
            try {
                inNewTransaction(() -> {
                    store.markFailed(messageId, consumerName, e.getMessage());
                    return null;
                });
            } catch (RuntimeException failureRecordingException) {
                e.addSuppressed(failureRecordingException);
            }
            throw e;
        }
    }

    private boolean executeWithoutTransaction(String messageId, String consumerName, InboxHandler handler) {
        if (!store.tryStartProcessing(messageId, consumerName)) {
            return false;
        }
        try {
            handler.handle();
            store.markProcessed(messageId, consumerName);
            return true;
        } catch (RuntimeException e) {
            store.markFailed(messageId, consumerName, e.getMessage());
            throw e;
        }
    }

    private <T> T inNewTransaction(TransactionCallback<T> callback) {
        try {
            return transactionRunner.call(TransactionOptions.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .build(), callback);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Inbox transaction failed", exception);
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
