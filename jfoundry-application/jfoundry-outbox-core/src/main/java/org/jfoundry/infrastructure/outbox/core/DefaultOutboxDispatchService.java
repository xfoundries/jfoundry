package org.jfoundry.infrastructure.outbox.core;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
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

    private final OutboxRepository repository;
    private final MessageSender messageSender;
    private final int maxRetries;
    private final BackoffStrategy backoff;
    private final String claimerId;

    public DefaultOutboxDispatchService(OutboxRepository repository,
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
        List<OutboxEntry> entries = repository.claimDispatchable(batchSize, claimerId);
        for (OutboxEntry entry : entries) {
            dispatchEntry(entry);
        }
    }

    private void dispatchEntry(OutboxEntry entry) {
        try {
            SendResult result = messageSender.send(entry.getTopic(), entry.getPayloadKey(), entry.getPayloadJson());
            if (result.success()) {
                repository.markAsPublished(entry.getEventId());
            } else {
                repository.markAsFailed(entry.getEventId(), result.errorMessage(), maxRetries, backoff);
            }
        } catch (RuntimeException e) {
            log.warn("dispatch entry {} failed with exception: {}", entry.getEventId(), e.getMessage());
            repository.markAsFailed(entry.getEventId(), e.getMessage(), maxRetries, backoff);
        }
    }
}
