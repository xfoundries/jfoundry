package org.jfoundry.application.inbox;

import java.util.Objects;

public class InboxTemplate {

    private final InboxMessageStore store;

    public InboxTemplate(InboxMessageStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    public boolean executeOnce(String messageId, String consumerName, InboxHandler handler) {
        requireText(messageId, "messageId");
        requireText(consumerName, "consumerName");
        Objects.requireNonNull(handler, "handler must not be null");

        if (store.isProcessed(messageId, consumerName)) {
            return false;
        }
        handler.handle();
        store.markProcessed(messageId, consumerName);
        return true;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
