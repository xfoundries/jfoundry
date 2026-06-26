package org.jfoundry.application.inbox;

public interface InboxMessageStore {

    boolean isProcessed(String messageId, String consumerName);

    void markProcessed(String messageId, String consumerName);
}
