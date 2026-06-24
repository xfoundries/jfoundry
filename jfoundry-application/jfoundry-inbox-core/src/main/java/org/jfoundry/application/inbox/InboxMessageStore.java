package org.jfoundry.application.inbox;

import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface InboxMessageStore {

    boolean isProcessed(String messageId, String consumerName);

    void markProcessed(String messageId, String consumerName);
}
