package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;

import java.time.Instant;
import java.util.List;

/// Outbox 表 MyBatis-Plus Mapper。
/// <p>
/// 继承 {@link BaseMapper} 标准能力（insert/selectById/updateById 等），并在 P2-1 中追加
/// 原子 claim + stuck 恢复相关的自定义 SQL：
/// <ul>
///   <li>{@link #claimPending(int, String, String)}：MySQL/H2 方言，{@code UPDATE ... WHERE id IN (LIMIT subquery)}。
///       使用 {@code CURRENT_TIMESTAMP} 而非 {@code NOW(3)} 以兼容 H2 测试与 MySQL 生产。</li>
///   <li>{@link #claimPendingDm(int, String, String)}：达梦方言，用 {@code ROWNUM <= #{limit}} 替代 {@code LIMIT}。</li>
///   <li>{@link #selectByClaimToken(String)}：claim 完成后，按 {@code claim_token} 精确回读刚 claim 的条目。
///       P3-2 前：按 {@code claimed_by + DISPATCHING} 回读会把同 pod 前一批未完成的记录一起带回
///       （根因：pod 内重入 dispatch，或 markAsPublished/markAsFailed 状态更新失败留下 DISPATCHING 残骸）。</li>
///   <li>{@link #resetStuckDispatching(Instant)}：P2-1 recovery job 的底层 UPDATE，
///       把 {@code claimed_at < cutoff} 的 DISPATCHING 记录回滚为 PENDING。</li>
///   <li>{@link #ageClaimedAt(String, Instant)}：测试专用，覆写 claimed_at 模拟陈旧 claim。</li>
/// </ul>
/// <p>
/// 方言分派（默认走 MySQL/H2）由 {@code MybatisPlusOutboxRepository.claimDispatchable} 决定；
/// 显式 DbType 注入在 P2-3 / Task 2.5 完成。
/// <p>
/// 实体类型为 {@link OutboxData}（MP 持久化视图），SPI 层 {@code OutboxEntry} 由
/// {@link MybatisPlusOutboxRepository} 在边界处互转。
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxData> {

    /// MySQL/H2 方言：原子 UPDATE...LIMIT + 后续 SELECT。
    /// <p>
    /// {@code UPDATE ... WHERE (status='PENDING' OR retry-due FAILED) ORDER BY event_id LIMIT N}
    /// 是 MySQL/H2 的原子 top-N claim 惯用法 —— UPDATE 语句在执行时对匹配的行加排他锁，
    /// 按 ORDER BY 顺序处理 LIMIT N 行；两个并发 UPDATE 会被行级锁串行化，后执行者看到的
    /// 候选集合已经不再包含前者 claim 走的行，因此自动选取不同的行。
    /// <p>
    /// 候选集语义与 {@link com.baomidou.mybatisplus.core.mapper.BaseMapper#selectPage} 形态的
    /// {@code findDispatchable} 对齐：覆盖 PENDING 全部 + FAILED 已到 {@code next_retry_at} 的
    /// 那部分。切到 claim 模式后，FAILED 重试语义不会丢失。
    /// <p>
    /// 与 "UPDATE...WHERE id IN (SELECT LIMIT N)" 的关键差异：后者 inner SELECT 的候选集
    /// 是语句开始时的快照，当 Tx A 已 claim 走候选集内若干行时，Tx B 的 outer UPDATE 即使
    /// 加了 outer status=PENDING 守卫，也会因为候选集耗尽而拿不到剩余 PENDING —— 需要 retry
    /// 循环。UPDATE...LIMIT 把候选集选取与加锁合并为单一原子操作，无需 retry。
    /// <p>
    /// {@code CURRENT_TIMESTAMP} 在 MySQL/H2 均可用；放弃 MySQL-only 的 {@code NOW(3)}。
    /// <p>
    /// P3-2: 每次调用都把 {@code claimToken}（调用方传入的唯一 UUID）写入 {@code claim_token}
    /// 列；后续 {@link #selectByClaimToken} 按该 token 精确回读本批条目。
    @Update("""
            UPDATE ddd_outbox_event
            SET status = 'DISPATCHING',
                claimed_at = CURRENT_TIMESTAMP,
                claimed_by = #{claimerId},
                claim_token = #{claimToken}
            WHERE (status = 'PENDING'
                   OR (status = 'FAILED'
                       AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)))
            ORDER BY event_id ASC
            LIMIT #{limit}
            """)
    int claimPending(@Param("limit") int limit,
                     @Param("claimerId") String claimerId,
                     @Param("claimToken") String claimToken);

    /// 达梦方言：DM 不支持 UPDATE...LIMIT，改用 ROWNUM 子查询（候选集快照模式）。
    /// <p>
    /// 暂未启用，方言分派在 Task 2.5 接入。并发场景下若 inner SELECT 的候选集被其他实例
    /// 先 claim 走，需要 Repository 层 retry（与原 UPDATE...IN (LIMIT) 方案相同的限制）。
    /// <p>
    /// 候选集语义与 MySQL/H2 版本一致：PENDING 全部 + FAILED 已到 {@code next_retry_at}。
    /// <p>
    /// P3-2: 同样把 {@code claimToken} 写入 {@code claim_token} 列。
    @Update("""
            UPDATE ddd_outbox_event
            SET status = 'DISPATCHING',
                claimed_at = CURRENT_TIMESTAMP,
                claimed_by = #{claimerId},
                claim_token = #{claimToken}
            WHERE (status = 'PENDING'
                   OR (status = 'FAILED'
                       AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)))
              AND event_id IN (
                SELECT id FROM (
                    SELECT event_id AS id FROM ddd_outbox_event
                    WHERE (status = 'PENDING'
                           OR (status = 'FAILED'
                               AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)))
                    AND ROWNUM <= #{limit}
                    ORDER BY event_id
                ) t
            )
            """)
    int claimPendingDm(@Param("limit") int limit,
                       @Param("claimerId") String claimerId,
                       @Param("claimToken") String claimToken);

    /// P3-2: claim 完成后按 {@code claimToken} 精确回读本批条目。
    /// <p>
    /// 关键不变量：{@code claimToken} 由调用方在每次 {@code claimDispatchable} 调用时现生成
    /// （UUID），因此即使同一 pod 重入 dispatch，或前一批的状态更新失败留下 DISPATCHING 残骸，
    /// 旧记录的 {@code claim_token} 也与新批不同，结果集天然隔离 —— 不会重复发送。
    /// <p>
    /// 仅匹配 DISPATCHING 状态：token 离开 DISPATCHING 时（markPublished/markFailed/reactivate
    /// /resetStuckDispatching）会被清空，因此过期 token 查询自然返回空。
    @Select("SELECT * FROM ddd_outbox_event WHERE claim_token = #{claimToken} AND status = 'DISPATCHING'")
    List<OutboxData> selectByClaimToken(@Param("claimToken") String claimToken);

    /// P2-1 stuck-DISPATCHING 恢复：把 {@code claimed_at < cutoff} 的 DISPATCHING 记录
    /// 回滚为 PENDING，清空 claimed_at / claimed_by / claim_token。
    /// <p>
    /// 单条 UPDATE 跨多行，由数据库行级锁保证原子性；没有候选集快照问题
    /// （不像 {@link #claimPendingDm} 的子查询形态）。
    /// <p>
    /// 场景：pod 在 DISPATCHING 中途崩溃，周期性 recovery job 调用本方法回收半完成记录。
    /// 同一 SQL 兼容 MySQL / H2 / 达梦。
    @Update("UPDATE ddd_outbox_event " +
            "SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL, claim_token = NULL " +
            "WHERE status = 'DISPATCHING' AND claimed_at < #{cutoff}")
    int resetStuckDispatching(@Param("cutoff") Instant cutoff);

    /// 测试辅助：直接覆写指定 event 的 claimed_at，用于模拟 "pod 在 DISPATCHING 中
    /// 崩溃后留下的陈旧 claim 时间戳"。生产代码不得调用。
    @Update("UPDATE ddd_outbox_event SET claimed_at = #{oldTimestamp} WHERE event_id = #{eventId}")
    void ageClaimedAt(@Param("eventId") String eventId, @Param("oldTimestamp") Instant oldTimestamp);

    /// P2-5 cleanup: 批量删除指定终态（PUBLISHED / DEAD_LETTERED）且 occurred_at 早于 cutoff
    /// 的记录，单批最多 {@code batchSize} 条。
    /// <p>
    /// 子查询 + LIMIT 形式兼容 MySQL 与 H2；DM 方言需要 ROWNUM 改写（暂未启用，方言分派
    /// 在后续任务接入）。子查询保证 {@code DELETE} 不受 MySQL/H2 早期版本对
    /// {@code DELETE...LIMIT} 支持差异影响。
    /// <p>
    /// {@code ORDER BY event_id ASC} 确保多批循环删除时顺序稳定，避免同一条记录
    /// 被多次扫描（虽然 DELETE 是幂等的，但稳定顺序便于测试断言与运维观测）。
    @Delete("""
            DELETE FROM ddd_outbox_event
            WHERE event_id IN (
                SELECT event_id FROM ddd_outbox_event
                WHERE status = #{status} AND occurred_at < #{cutoff}
                ORDER BY event_id ASC
                LIMIT #{batchSize}
            )
            """)
    int deleteBatchByStatusAndOccurredBefore(@Param("status") String status,
                                             @Param("cutoff") Instant cutoff,
                                             @Param("batchSize") int batchSize);

    /// 测试辅助：直接覆写指定 event 的 status，用于把 {@code OutboxEntry.newPending}
    /// 创建的 PENDING 记录强制置为 PUBLISHED / DEAD_LETTERED 终态，供 cleanup job 测试。
    /// 生产代码不得调用——生产链路中状态流转只能通过 {@code markPublished} /
    /// {@code markFailed(...)} 完成。
    @Update("UPDATE ddd_outbox_event SET status = #{status} WHERE event_id = #{eventId}")
    void updateStatus(@Param("eventId") String eventId, @Param("status") OutboxStatus status);
}
