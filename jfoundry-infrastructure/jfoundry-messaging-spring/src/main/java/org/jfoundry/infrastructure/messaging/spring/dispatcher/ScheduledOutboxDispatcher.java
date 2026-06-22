package org.jfoundry.infrastructure.messaging.spring.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxDispatcher;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.List;

/// 默认 OutboxDispatcher 实现：通过 Spring {@code @Scheduled} 周期性投递。
/// <p>
/// 每次 dispatch：
/// <ol>
///   <li>从 {@link OutboxRepository#findDispatchable(int, Instant)} 取出 batchSize 条记录。</li>
///   <li>逐条调用 {@link MessageSender#send(String, String, String)}。</li>
///   <li>成功 → {@link OutboxRepository#markAsPublished(String)}；失败 →
///       {@link OutboxRepository#markAsFailed(String, String, int, BackoffStrategy)}。</li>
/// </ol>
/// 单条失败不会阻塞后续条目（catch 在 for 循环内部）。
public class ScheduledOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledOutboxDispatcher.class);

    private final OutboxRepository repository;
    private final MessageSender messageSender;
    private final int maxRetries;
    private final BackoffStrategy backoff;
    private final int batchSize;

    public ScheduledOutboxDispatcher(OutboxRepository repository,
                                      MessageSender messageSender,
                                      int maxRetries,
                                      BackoffStrategy backoff,
                                      int batchSize) {
        this.repository = repository;
        this.messageSender = messageSender;
        this.maxRetries = maxRetries;
        this.backoff = backoff;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${jfoundry.outbox.dispatcher.interval-ms:5000}")
    public void scheduledDispatch() {
        dispatch(batchSize);
    }

    @Override
    public void dispatch(int batchSize) {
        List<OutboxEntry> entries = repository.findDispatchable(batchSize, Instant.now());
        for (OutboxEntry entry : entries) {
            try {
                SendResult result = messageSender.send(entry.getTopic(), entry.getPayloadKey(), entry.getPayloadJson());
                if (result.success()) {
                    repository.markAsPublished(entry.getEventId());
                } else {
                    repository.markAsFailed(entry.getEventId(), result.errorMessage(), maxRetries, backoff);
                }
            } catch (RuntimeException e) {
                log.warn("dispatch entry {} 抛出异常，按失败处理：{}", entry.getEventId(), e.getMessage());
                repository.markAsFailed(entry.getEventId(), e.getMessage(), maxRetries, backoff);
            }
        }
    }
}
