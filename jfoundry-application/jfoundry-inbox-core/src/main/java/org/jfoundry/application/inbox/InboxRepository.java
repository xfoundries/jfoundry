package org.jfoundry.application.inbox;

public interface InboxRepository {

    boolean isProcessed(String messageId, String consumerName);

    void markProcessed(String messageId, String consumerName);
}
