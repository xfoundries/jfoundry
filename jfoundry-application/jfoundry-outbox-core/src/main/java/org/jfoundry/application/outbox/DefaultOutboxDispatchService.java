package org.jfoundry.application.outbox;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/// Framework-neutral Outbox dispatch runtime.
/// <p>
/// Framework adapters decide when this service is triggered. This service owns the shared
/// claim/send/mark state transition so Spring, JobRunr, Helidon, or Quarkus integrations do
/// not duplicate delivery behavior.
public class DefaultOutboxDispatchService implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDispatchService.class);

    private final OutboxMessageStore repository;
    private final MessageSender messageSender;
    private final int maxRetries;
    private final BackoffStrategy backoff;
    private final String claimerId;

    public DefaultOutboxDispatchService(OutboxMessageStore repository,
                                        MessageSender messageSender,
                                        int maxRetries,
                                        BackoffStrategy backoff,
                                        String claimerId) {
        this.repository = repository;
        this.messageSender = messageSender;
        this.maxRetries = maxRetries;
        this.backoff = backoff;
        this.claimerId = claimerId;
    }

    @Override
    public void dispatch(int batchSize) {
        List<OutboxMessage> messages = repository.claimDispatchable(batchSize, claimerId);
        for (OutboxMessage message : messages) {
            dispatchMessage(message);
        }
    }

    private void dispatchMessage(OutboxMessage message) {
        try {
            SendResult result = messageSender.send(message.getTopic(), message.getPayloadKey(), message.getPayloadJson());
            if (result.success()) {
                repository.markAsPublished(message.getEventId());
            } else {
                repository.markAsFailed(message.getEventId(), result.errorMessage(), maxRetries, backoff);
            }
        } catch (RuntimeException e) {
            log.warn("dispatch message {} failed with exception: {}", message.getEventId(), e.getMessage());
            repository.markAsFailed(message.getEventId(), e.getMessage(), maxRetries, backoff);
        }
    }
}
