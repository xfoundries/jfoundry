package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;

/// Outbox 表 MyBatis-Plus Mapper。
/// <p>
/// 继承 {@link BaseMapper} 标准能力（insert/selectById/updateById 等），并在 P2-1 中追加
/// 原子 claim + stuck 恢复相关的自定义 SQL：
/// <ul>
///   <li>{@link #claimPending(int, String)}：MySQL/H2 方言，{@code UPDATE ... WHERE id IN (LIMIT subquery)}。
///       使用 {@code CURRENT_TIMESTAMP} 而非 {@code NOW(3)} 以兼容 H2 测试与 MySQL 生产。</li>
///   <li>{@link #claimPendingDm(int, String)}：达梦方言，用 {@code ROWNUM <= #{limit}} 替代 {@code LIMIT}。</li>
///   <li>{@link #selectByClaimer(String)}：claim 完成后，按 {@code claimed_by + DISPATCHING} 回读刚 claim 的条目。</li>
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
    /// {@code UPDATE ... WHERE status='PENDING' ORDER BY event_id LIMIT N} 是 MySQL/H2 的
    /// 原子 top-N claim 惯用法 —— UPDATE 语句在执行时对匹配的行加排他锁，按 ORDER BY 顺序
    /// 处理 LIMIT N 行；两个并发 UPDATE 会被行级锁串行化，后执行者看到的 PENDING 集合已经
    /// 不再包含前者 claim 走的行，因此自动选取不同的行。
    /// <p>
    /// 与 "UPDATE...WHERE id IN (SELECT LIMIT N)" 的关键差异：后者 inner SELECT 的候选集
    /// 是语句开始时的快照，当 Tx A 已 claim 走候选集内若干行时，Tx B 的 outer UPDATE 即使
    /// 加了 outer status=PENDING 守卫，也会因为候选集耗尽而拿不到剩余 PENDING —— 需要 retry
    /// 循环。UPDATE...LIMIT 把候选集选取与加锁合并为单一原子操作，无需 retry。
    /// <p>
    /// {@code CURRENT_TIMESTAMP} 在 MySQL/H2 均可用；放弃 MySQL-only 的 {@code NOW(3)}。
    @Update("""
            UPDATE ddd_outbox_event
            SET status = 'DISPATCHING',
                claimed_at = CURRENT_TIMESTAMP,
                claimed_by = #{claimerId}
            WHERE status = 'PENDING'
            ORDER BY event_id ASC
            LIMIT #{limit}
            """)
    int claimPending(@Param("limit") int limit, @Param("claimerId") String claimerId);

    /// 达梦方言：DM 不支持 UPDATE...LIMIT，改用 ROWNUM 子查询（候选集快照模式）。
    /// <p>
    /// 暂未启用，方言分派在 Task 2.5 接入。并发场景下若 inner SELECT 的候选集被其他实例
    /// 先 claim 走，需要 Repository 层 retry（与原 UPDATE...IN (LIMIT) 方案相同的限制）。
    @Update("""
            UPDATE ddd_outbox_event
            SET status = 'DISPATCHING',
                claimed_at = CURRENT_TIMESTAMP,
                claimed_by = #{claimerId}
            WHERE status = 'PENDING'
              AND event_id IN (
                SELECT id FROM (
                    SELECT event_id AS id FROM ddd_outbox_event
                    WHERE status = 'PENDING' AND ROWNUM <= #{limit}
                    ORDER BY event_id
                ) t
            )
            """)
    int claimPendingDm(@Param("limit") int limit, @Param("claimerId") String claimerId);

    /// claim 完成后回读：按 claimed_by + DISPATCHING 取出本 pod 刚 claim 的条目。
    /// <p>
    /// 注意：该查询只会在同一线程/事务内看到自己刚 UPDATE 的行；并发场景下不同 pod
    /// 的 claimed_by 互不相同，因此结果集天然隔离。
    @Select("SELECT * FROM ddd_outbox_event WHERE claimed_by = #{claimerId} AND status = 'DISPATCHING'")
    List<OutboxData> selectByClaimer(@Param("claimerId") String claimerId);

    /// P2-1 stuck-DISPATCHING 恢复：把 {@code claimed_at < cutoff} 的 DISPATCHING 记录
    /// 回滚为 PENDING，清空 claimed_at / claimed_by。
    /// <p>
    /// 单条 UPDATE 跨多行，由数据库行级锁保证原子性；没有候选集快照问题
    /// （不像 {@link #claimPendingDm} 的子查询形态）。
    /// <p>
    /// 场景：pod 在 DISPATCHING 中途崩溃，周期性 recovery job 调用本方法回收半完成记录。
    /// 同一 SQL 兼容 MySQL / H2 / 达梦。
    @Update("UPDATE ddd_outbox_event " +
            "SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL " +
            "WHERE status = 'DISPATCHING' AND claimed_at < #{cutoff}")
    int resetStuckDispatching(@Param("cutoff") Instant cutoff);

    /// 测试辅助：直接覆写指定 event 的 claimed_at，用于模拟 "pod 在 DISPATCHING 中
    /// 崩溃后留下的陈旧 claim 时间戳"。生产代码不得调用。
    @Update("UPDATE ddd_outbox_event SET claimed_at = #{oldTimestamp} WHERE event_id = #{eventId}")
    void ageClaimedAt(@Param("eventId") String eventId, @Param("oldTimestamp") Instant oldTimestamp);
}
