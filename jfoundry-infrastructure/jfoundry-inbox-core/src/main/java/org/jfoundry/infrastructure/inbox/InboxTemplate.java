package org.jfoundry.infrastructure.inbox;

import java.util.Objects;

public class InboxTemplate {

    private final InboxRepository repository;

    public InboxTemplate(InboxRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public boolean executeOnce(String messageId, String consumerName, InboxHandler handler) {
        requireText(messageId, "messageId");
        requireText(consumerName, "consumerName");
        Objects.requireNonNull(handler, "handler must not be null");

        if (repository.isProcessed(messageId, consumerName)) {
            return false;
        }
        handler.handle();
        repository.markProcessed(messageId, consumerName);
        return true;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
