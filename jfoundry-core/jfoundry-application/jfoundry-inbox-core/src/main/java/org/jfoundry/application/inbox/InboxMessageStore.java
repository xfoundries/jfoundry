package org.jfoundry.application.inbox;

public interface InboxMessageStore {

    boolean isProcessed(String messageId, String consumerName);

    default boolean tryStartProcessing(String messageId, String consumerName) {
        return !isProcessed(messageId, consumerName);
    }

    void markProcessed(String messageId, String consumerName);

    default void markFailed(String messageId, String consumerName, String errorMessage) {
        // Backward-compatible default for existing custom stores.
    }
}
