package org.jfoundry.infrastructure.outbox.spring.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.OutboxDispatcher;
import org.jfoundry.infrastructure.outbox.core.OutboxEntry;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/// 默认 OutboxDispatcher 实现：通过 Spring {@code @Scheduled} 周期性投递。
/// <p>
/// 每次 dispatch：
/// <ol>
///   <li>调用 {@link OutboxRepository#claimDispatchable(int, String)} 原子 claim batchSize 条
///       记录（PENDING + retry-due FAILED），claim 后这些记录属于本 pod。</li>
///   <li>逐条调用 {@link MessageSender#send(String, String, String)}。</li>
///   <li>成功 → {@link OutboxRepository#markAsPublished(String)}；失败 →
///       {@link OutboxRepository#markAsFailed(String, String, int, BackoffStrategy)}。
///       两条路径都在 {@link OutboxEntry} 内清空 claimed_at/claimed_by，释放 claim。</li>
/// </ol>
/// 单条失败不会阻塞后续条目（catch 在 for 循环内部）。
/// <p>
/// 多实例互斥由 claim 的原子 UPDATE...LIMIT 保证：两个 pod 并发 dispatch 不会拿到同一条记录。
public class ScheduledOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledOutboxDispatcher.class);

    private final OutboxRepository repository;
    private final MessageSender messageSender;
    private final int maxRetries;
    private final BackoffStrategy backoff;
    private final int batchSize;
    /// 稳定的 pod 标识（hostname + 短 UUID），跨 dispatch 调用复用，便于 stuck-recovery 诊断
    /// 与运维观测。{@link OutboxRepository#claimDispatchable(int, String)} 内部用它做
    /// UPDATE...WHERE claimed_by=#{claimerId}；回读使用每次调用现生成的 claimToken
    /// （而非稳定 podId），避免重入或残骸导致重复发送。
    private final String podId;

    public ScheduledOutboxDispatcher(OutboxRepository repository,
                                      MessageSender messageSender,
                                      int maxRetries,
                                      BackoffStrategy backoff,
                                      int batchSize) {
        this(repository, messageSender, maxRetries, backoff, batchSize, generatePodId());
    }

    /// 测试专用：允许注入 podId 以断言并发互斥行为。生产构造函数走 {@link #generatePodId()}。
    public ScheduledOutboxDispatcher(OutboxRepository repository,
                                     MessageSender messageSender,
                                     int maxRetries,
                                     BackoffStrategy backoff,
                                     int batchSize,
                                     String podId) {
        this.repository = repository;
        this.messageSender = messageSender;
        this.maxRetries = maxRetries;
        this.backoff = backoff;
        this.batchSize = batchSize;
        this.podId = podId;
    }

    public static String generatePodId() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Scheduled(fixedDelayString = "${jfoundry.outbox.dispatcher.interval-ms:5000}")
    public void scheduledDispatch() {
        dispatch(batchSize);
    }

    @Override
    public void dispatch(int batchSize) {
        List<OutboxEntry> entries = repository.claimDispatchable(batchSize, podId);
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
