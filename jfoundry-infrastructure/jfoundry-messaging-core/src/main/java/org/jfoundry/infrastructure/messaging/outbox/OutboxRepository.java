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

    /// 原子声明一批待派发事件（多实例安全）。
    /// <p>
    /// 实现 SQL 形如（MySQL/H2 方言）：
    /// <pre>
    /// UPDATE outbox_event
    ///   SET status = 'DISPATCHING', claimed_at = now, claimed_by = #{claimerId}
    ///   WHERE id IN (SELECT id FROM (SELECT id FROM outbox_event
    ///                                 WHERE status = 'PENDING' LIMIT #{limit}) t);
    /// SELECT * FROM outbox_event WHERE claimed_by = #{claimerId} AND status = 'DISPATCHING';
    /// </pre>
    /// <p>
    /// 多实例下，两个并发 claim 不会拿到相同记录（数据库行级锁保证）。
    /// <p>
    /// 实现侧注意：达梦数据库不支持 UPDATE...LIMIT subquery，需走 ROWNUM/top-N 改写
    /// （由 P2-3 dialect 机制分派）。
    /// <p>
    /// 参数约束：
    /// <ul>
    ///   <li>{@code limit <= 0} 抛 {@link IllegalArgumentException}</li>
    ///   <li>{@code claimerId} 为 null 或空白抛 {@link IllegalArgumentException}</li>
    /// </ul>
    List<OutboxEntry> claimDispatchable(int limit, String claimerId);
}
