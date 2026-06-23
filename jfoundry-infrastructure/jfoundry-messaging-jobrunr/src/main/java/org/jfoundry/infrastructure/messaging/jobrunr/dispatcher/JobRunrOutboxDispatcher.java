package org.jfoundry.infrastructure.messaging.jobrunr.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxDispatcher;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.spring.dispatcher.ScheduledOutboxDispatcher;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.spring.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/// 基于 JobRunr 的 Outbox Dispatcher 实现。
/// <p>
/// Bean 注册由 {@link JobRunrDispatcherAutoConfiguration} 负责（{@code @AutoConfiguration}），
/// 启用条件：JobRunr 在 classpath 且 {@code jfoundry.outbox.dispatcher.mode=jobrunr}。
/// bean 名 "jobRunrOutboxDispatcher" 是 ScheduledOutboxDispatcher 让位的互斥键，重命名会破坏互斥。
/// <p>
/// Claim 流程与 {@link ScheduledOutboxDispatcher} 一致：先原子
/// {@link OutboxRepository#claimDispatchable(int, String)} 拿到本 pod 的条目，
/// 再 send/markAsPublished/markAsFailed；多实例互斥由 claim 的原子 UPDATE...LIMIT 保证。
public class JobRunrOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobRunrOutboxDispatcher.class);

    private final OutboxRepository outboxRepository;
    private final MessageSender messageSender;
    private final int batchSize;
    private final int maxRetries;
    private final BackoffStrategy backoff;
    private final String podId;

    public JobRunrOutboxDispatcher(OutboxRepository outboxRepository,
                                    MessageSender messageSender,
                                    int batchSize,
                                    int maxRetries,
                                    BackoffStrategy backoff) {
        this(outboxRepository, messageSender, batchSize, maxRetries, backoff,
                ScheduledOutboxDispatcher.generatePodId());
    }

    /// 测试专用：允许注入 podId 以断言并发互斥行为。生产构造函数走
    /// {@link ScheduledOutboxDispatcher#generatePodId()}。
    JobRunrOutboxDispatcher(OutboxRepository outboxRepository,
                            MessageSender messageSender,
                            int batchSize,
                            int maxRetries,
                            BackoffStrategy backoff,
                            String podId) {
        this.outboxRepository = outboxRepository;
        this.messageSender = messageSender;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.backoff = backoff;
        this.podId = podId;
    }

    @Job(name = "outbox-dispatch", retries = 3)
    @Recurring(id = "jfoundry-outbox-dispatch", cron = "${jfoundry.outbox.dispatcher.cron:*/10 * * * * *}")
    public void recurringDispatch() {
        dispatchInternal(batchSize);
    }

    @Override
    public void dispatch(int batchSize) {
        dispatchInternal(batchSize);
    }

    private void dispatchInternal(int batchSize) {
        List<OutboxEntry> pending = outboxRepository.claimDispatchable(batchSize, podId);
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
