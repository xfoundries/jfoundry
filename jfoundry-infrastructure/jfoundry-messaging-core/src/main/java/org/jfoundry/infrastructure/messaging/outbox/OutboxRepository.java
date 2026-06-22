package org.jfoundry.infrastructure.messaging.outbox;

import java.time.Instant;
import java.util.List;

/// Outbox 持久化 SPI。
/// <p>
/// 实现侧契约（read-then-update 模式）：
/// <ul>
///   <li>{@link #markAsPublished(String)} / {@link #markAsFailed(String, String, int, BackoffStrategy)}
///       / {@link #reactivate(String)} 均为 load → mutate → save。entry 不存在时静默返回。</li>
///   <li>{@link #reactivate(String)} 当 entry 非 DEAD_LETTERED 时由 {@link OutboxEntry#reactivate()}
///       抛 {@link IllegalStateException}（fail-fast）。</li>
/// </ul>
/// 多实例安全性：v1 不实现分布式锁，依赖消费端幂等（详见 spec §5.8）。
public interface OutboxRepository {

    void append(OutboxEntry entry);

    /// 取出待 dispatch 的条目：
    /// <pre>
    /// status IN (PENDING, FAILED) AND (next_retry_at IS NULL OR next_retry_at <= now)
    /// ORDER BY occurredAt ASC
    /// LIMIT n
    /// </pre>
    List<OutboxEntry> findDispatchable(int limit, Instant now);

    void markAsPublished(String eventId);

    void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff);

    void reactivate(String eventId);
}
