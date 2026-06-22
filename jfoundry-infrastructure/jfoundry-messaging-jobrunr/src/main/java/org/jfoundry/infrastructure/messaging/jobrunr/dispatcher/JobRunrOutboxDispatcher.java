package org.jfoundry.infrastructure.messaging.jobrunr.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxDispatcher;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.spring.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/// 基于 JobRunr 的 Outbox Dispatcher 实现。
/// <p>
/// 启用条件：JobRunr 在 classpath 且 ddd.outbox.dispatcher.mode=jobrunr。
/// bean 名 "jobRunrOutboxDispatcher" 是 ScheduledOutboxDispatcher 让位的互斥键，重命名会破坏互斥。
@Component("jobRunrOutboxDispatcher")
@ConditionalOnClass(name = "org.jobrunr.jobs.annotations.Job")
@ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "mode", havingValue = "jobrunr")
public class JobRunrOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobRunrOutboxDispatcher.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_MAX_RETRIES = 5;

    private final OutboxRepository outboxRepository;
    private final MessageSender messageSender;
    private final int maxRetries;
    private final BackoffStrategy backoff;

    public JobRunrOutboxDispatcher(OutboxRepository outboxRepository,
                                    MessageSender messageSender,
                                    BackoffStrategy backoff) {
        this(outboxRepository, messageSender, DEFAULT_MAX_RETRIES, backoff);
    }

    public JobRunrOutboxDispatcher(OutboxRepository outboxRepository,
                                    MessageSender messageSender,
                                    int maxRetries,
                                    BackoffStrategy backoff) {
        this.outboxRepository = outboxRepository;
        this.messageSender = messageSender;
        this.maxRetries = maxRetries;
        this.backoff = backoff;
    }

    @Job(name = "outbox-dispatch", retries = 3)
    @Recurring(id = "ddd-outbox-dispatch", cron = "${ddd.outbox.dispatcher.cron:*/10 * * * * *}")
    public void recurringDispatch() {
        dispatchInternal(DEFAULT_BATCH_SIZE);
    }

    @Override
    public void dispatch(int batchSize) {
        dispatchInternal(batchSize);
    }

    private void dispatchInternal(int batchSize) {
        List<OutboxEntry> pending = outboxRepository.findDispatchable(batchSize, Instant.now());
        if (pending.isEmpty()) {
            return;
        }
        log.debug("[OUTBOX-DISPATCH-JOBRUNR] processing {} entries", pending.size());
        for (OutboxEntry entry : pending) {
            try {
                SendResult result = messageSender.send(
                        entry.getTopic(), entry.getPayloadKey(), entry.getPayloadJson());
                if (result.success()) {
                    outboxRepository.markAsPublished(entry.getEventId());
                } else {
                    outboxRepository.markAsFailed(
                            entry.getEventId(), result.errorMessage(), maxRetries, backoff);
                }
            } catch (RuntimeException e) {
                log.warn("[OUTBOX-DISPATCH-JOBRUNR] send failed, eventId={}, error={}",
                        entry.getEventId(), e.getMessage());
                outboxRepository.markAsFailed(
                        entry.getEventId(), e.getMessage(), maxRetries, backoff);
            }
        }
    }
}
