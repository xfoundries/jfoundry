package org.jfoundry.application.outbox;

import java.time.Instant;
import java.util.List;

/// Outbox 持久化 SPI。
/// <p>
/// 实现侧契约（read-then-update 模式）：
/// <ul>
///   <li>{@link #markAsPublished(String)} / {@link #markAsFailed(String, String, int, BackoffStrategy)}
///       / {@link #reactivate(String)} 均为 load → mutate → save。entry 不存在时静默返回。</li>
///   <li>{@link #reactivate(String)} 当 entry 非 DEAD_LETTERED 时由 {@link OutboxMessage#reactivate()}
///       抛 {@link IllegalStateException}（fail-fast）。</li>
/// </ul>
/// 多实例安全性：v1 不实现分布式锁，依赖消费端幂等（详见 spec §5.8）。
public interface OutboxMessageStore {

    void append(OutboxMessage entry);

    /// 取出待 dispatch 的条目：
    /// <pre>
    /// status IN (PENDING, FAILED) AND (next_retry_at IS NULL OR next_retry_at <= now)
    /// ORDER BY occurredAt ASC
    /// LIMIT n
    /// </pre>
    List<OutboxMessage> findDispatchable(int limit, Instant now);

    void markAsPublished(String eventId);

    default void markAsPublished(String eventId, String claimToken) {
        markAsPublished(eventId);
    }

    void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff);

    default void markAsFailed(String eventId, String claimToken,
                              String errorMessage, int maxRetries, BackoffStrategy backoff) {
        markAsFailed(eventId, errorMessage, maxRetries, backoff);
    }

    void reactivate(String eventId);

    /// 原子声明一批待派发事件（多实例安全）。
    /// <p>
    /// 实现必须保证同一条记录不会被两个并发 claimer 同时返回。推荐实现方式是先分页选取
    /// dispatchable 候选，再对每条候选执行 CAS update：
    /// <pre>
    /// UPDATE jfoundry_outbox_event
    ///   SET status = 'DISPATCHING', claimed_at = CURRENT_TIMESTAMP,
    ///       claimed_by = #{claimerId}, claim_token = #{claimToken}
    ///   WHERE event_id = #{eventId}
    ///     AND status = #{candidateStatus};
    /// </pre>
    /// <p>
    /// CAS 成功的记录才允许出现在返回值中；CAS 失败说明其它 claimer 已抢走该记录，应跳过并
    /// 继续补齐本批次。实现也可以使用数据库原生 top-N update/locking 语法，但对外语义必须一致。
    /// <p>
    /// 每次调用应生成唯一 {@code claimToken}（UUID 或等价随机标识）写入 {@code claim_token}
    /// 列，便于诊断本批 claim，并让 {@link #markAsPublished(String, String)} /
    /// {@link #markAsFailed(String, String, String, int, BackoffStrategy)} 能校验调用方仍持有
    /// 该记录。token 在离开 DISPATCHING 状态时应被清空。
    /// <p>
    /// 参数约束：
    /// <ul>
    ///   <li>{@code limit <= 0} 抛 {@link IllegalArgumentException}</li>
    ///   <li>{@code claimerId} 为 null 或空白抛 {@link IllegalArgumentException}</li>
    /// </ul>
    List<OutboxMessage> claimDispatchable(int limit, String claimerId);

    /// 恢复卡住的 DISPATCHING 记录：claimedAt 早于 {@code cutoff} 的记录回滚为 PENDING。
    /// <p>
    /// 实现 SQL 形如（跨方言）：
    /// <pre>
    /// UPDATE jfoundry_outbox_event
    ///   SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL, claim_token = NULL
    ///   WHERE status = 'DISPATCHING' AND claimed_at &lt; #{cutoff};
    /// </pre>
    /// <p>
    /// 场景：pod 在 DISPATCHING 中途崩溃 / kill -9，记录残留在 DISPATCHING 状态。
    /// 周期性调用本方法（传入 {@code Instant.now().minus(stuckTimeout)}）即可回收。
    /// <p>
    /// 参数约束：
    /// <ul>
    ///   <li>{@code cutoff} 为 null 抛 {@link IllegalArgumentException}</li>
    /// </ul>
    /// @param cutoff 截止时刻，claimedAt 严格早于该时刻的 DISPATCHING 记录被回滚
    /// @return 回滚的记录数（0 表示没有卡住的记录）
    int recoverStuckDispatching(Instant cutoff);

    /// P2-5: 批量删除指定终态（PUBLISHED / DEAD_LETTERED）且 {@code occurredAt} 早于
    /// {@code cutoff} 的记录，单批最多 {@code batchSize} 条。
    /// <p>
    /// 实现侧契约：每批最多删除 {@code batchSize} 条，并循环到候选耗尽。可以使用
    /// selectPage + deleteByIds，也可以使用数据库原生批量删除语法；最终返回累计删除的记录总数。
    /// <p>
    /// 场景：Outbox 表中 PUBLISHED / DEAD_LETTERED 记录堆积会拖慢 claim/dispatch 查询，
    /// 周期性调用本方法（传入 {@code Instant.now().minus(retentionDays)}）即可按保留期清理。
    /// 任务幂等——重复执行无副作用；失败不影响 Outbox 主链路。
    /// <p>
    /// 参数约束：
    /// <ul>
    ///   <li>{@code status} 为 null 抛 {@link IllegalArgumentException}</li>
    ///   <li>{@code cutoff} 为 null 抛 {@link IllegalArgumentException}</li>
    ///   <li>{@code batchSize &lt;= 0} 抛 {@link IllegalArgumentException}</li>
    /// </ul>
    /// @param status    目标终态（PUBLISHED / DEAD_LETTERED）
    /// @param cutoff    截止时刻，occurredAt 严格早于该时刻的记录被删除
    /// @param batchSize 单批最多删除的记录数（实现侧循环到删干净）
    /// @return 累计删除的记录总数（0 表示没有匹配的记录）
    int deleteByStatusAndOccurredAtBefore(OutboxMessageStatus status, Instant cutoff, int batchSize);
}
