package org.jfoundry.infrastructure.outbox.core;

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
    /// 实现 SQL 形如（MySQL/H2 方言，{@code UPDATE...ORDER BY...LIMIT N} 单语句原子 top-N
    /// claim，候选集选取与加锁合并为单一原子操作，无需 retry）：
    /// <pre>
    /// UPDATE ddd_outbox_event
    ///   SET status = 'DISPATCHING', claimed_at = CURRENT_TIMESTAMP,
    ///       claimed_by = #{claimerId}, claim_token = #{claimToken}
    ///   WHERE status = 'PENDING'
    ///   ORDER BY event_id ASC
    ///   LIMIT #{limit};
    /// SELECT * FROM ddd_outbox_event
    ///   WHERE claim_token = #{claimToken} AND status = 'DISPATCHING';
    /// </pre>
    /// <p>
    /// 多实例下，两个并发 UPDATE 会被行级锁串行化，后执行者看到的 PENDING 集合已不再
    /// 包含前者 claim 走的行，因此自动选取不同记录（无需应用层 retry）。
    /// <p>
    /// <b>回读使用 claimToken 而非稳定 podId（P3-2）。</b>每次调用内部现生成唯一
    /// {@code claimToken}（UUID）写入 {@code claim_token} 列，回读按该 token 精确匹配。
    /// 旧实现按 {@code claimed_by + DISPATCHING} 回读，在 pod 内重入 dispatch（例如上一批
    /// 还在 send 循环中）或 {@code markAsPublished}/{@code markAsFailed} 状态更新失败留下
    /// DISPATCHING 残骸时，会把旧批记录一起带回 —— 导致重复发送。token 在离开 DISPATCHING
    /// 状态时（{@code markPublished} / {@code markFailed} / {@code reactivate} /
    /// {@code recoverStuckDispatching}）会被清空，过期 token 查询自然返回空。
    /// <p>
    /// 实现侧注意：达梦数据库不支持 UPDATE...LIMIT，需走 ROWNUM/top-N 子查询改写
    /// （由 P2-3 dialect 机制分派；候选集快照模式，并发下可能需 Repository 层 retry）。
    /// 具体 SQL 形态见 {@code jfoundry-outbox-mybatis-plus} 模块的
    /// {@code OutboxMapper.claimPending}。
    /// <p>
    /// 参数约束：
    /// <ul>
    ///   <li>{@code limit <= 0} 抛 {@link IllegalArgumentException}</li>
    ///   <li>{@code claimerId} 为 null 或空白抛 {@link IllegalArgumentException}</li>
    /// </ul>
    List<OutboxEntry> claimDispatchable(int limit, String claimerId);

    /// 恢复卡住的 DISPATCHING 记录：claimedAt 早于 {@code cutoff} 的记录回滚为 PENDING。
    /// <p>
    /// 实现 SQL 形如（跨方言）：
    /// <pre>
    /// UPDATE ddd_outbox_event
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
    /// 实现 SQL 形如（MySQL/H2 方言，子查询 + LIMIT 保证单批删干净且可跨方言）：
    /// <pre>
    /// DELETE FROM ddd_outbox_event
    ///   WHERE event_id IN (
    ///     SELECT event_id FROM ddd_outbox_event
    ///     WHERE status = #{status} AND occurred_at &lt; #{cutoff}
    ///     ORDER BY event_id ASC
    ///     LIMIT #{batchSize}
    ///   );
    /// </pre>
    /// <p>
    /// 实现侧契约：循环调用 mapper 的 batch delete，直到返回 &lt; batchSize，把所有匹配记录
    /// 删干净（每次循环最多删 batchSize 条）。最终返回累计删除的记录总数。
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
    int deleteByStatusAndOccurredAtBefore(OutboxStatus status, Instant cutoff, int batchSize);
}
