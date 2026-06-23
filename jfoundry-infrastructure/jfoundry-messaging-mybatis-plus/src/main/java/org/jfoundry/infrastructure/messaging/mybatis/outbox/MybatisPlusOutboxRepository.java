package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// OutboxRepository 默认实现：基于 MyBatis-Plus。
/// <p>
/// SPI 层 {@link OutboxEntry} 不携带任何 ORM 注解，本类在边界处负责 entry ↔ {@link OutboxData}
/// 互转。所有数据库操作走 {@link com.baomidou.mybatisplus.core.mapper.BaseMapper} 标准 API
/// （selectPage / update / deleteBatchIds 等），跨方言 SQL 由 MyBatis-Plus +
/// {@link PaginationInnerInterceptor} 在运行时按 {@link com.baomidou.mybatisplus.annotation.DbType}
/// 生成；本类不维护任何数据库特定 SQL，新增方言无需改源码。
/// <p>
/// <b>多实例原子 claim（核心）</b>：放弃 MySQL 特有的 {@code UPDATE...ORDER BY...LIMIT N}，
/// 改为 {@code selectPage(N) → 逐条 CAS UPDATE}：
/// <ol>
///   <li>{@code selectPage(N, WHERE status IN (PENDING, FAILED) AND retry-due)} 选取候选集，
///       LIMIT 由 PaginationInnerInterceptor 按方言生成。</li>
///   <li>对每条候选执行 {@code UPDATE...WHERE event_id=? AND status=candidate.status}（CAS 守卫）。
///       若并发 claimer 已抢走该条（{@code status} 变 DISPATCHING），CAS 失败（{@code affectedRows=0}），
///       跳过该条不发送。CAS UPDATE 是标准 ANSI SQL，跨方言。</li>
/// </ol>
/// 这种方式在 H2（READ_COMMITTED）下 select 天然不重叠，CAS 失败概率极低；在 REPEATABLE_READ
/// 或更高隔离级别下 CAS 提供兜底防御。{@code claimToken} 字段保留（每次调用现生成 UUID），
/// 用于运维观测与 P3-2 语义对齐，但 CAS 模式下不再依赖 token 做回读去重。
/// <p>
/// 构造时 fail-fast：检测传入的 MybatisPlusInterceptor 是否含 PaginationInnerInterceptor。
public class MybatisPlusOutboxRepository implements OutboxRepository {

    private final OutboxMapper mapper;

    public MybatisPlusOutboxRepository(OutboxMapper mapper,
                                       MybatisPlusInterceptor mybatisPlusInterceptor) {
        this.mapper = mapper;
        verifyPaginationInterceptor(mybatisPlusInterceptor);
    }

    private static void verifyPaginationInterceptor(MybatisPlusInterceptor interceptor) {
        boolean hasPagination = interceptor.getInterceptors().stream()
                .anyMatch(PaginationInnerInterceptor.class::isInstance);
        if (!hasPagination) {
            throw new IllegalStateException(
                    "MybatisPlusInterceptor 中未包含 PaginationInnerInterceptor，"
                            + "OutboxRepository 多处依赖 selectPage 生成方言 SQL（findDispatchable / "
                            + "claimDispatchable / deleteByStatusAndCreatedAtBefore），"
                            + "未注册时 selectPage 会 silently 返回全表。"
                            + "请将 PaginationInnerInterceptor 加入 MybatisPlusInterceptor。");
        }
    }

    @Override
    public void append(OutboxEntry entry) {
        mapper.insert(OutboxData.fromEntry(entry));
    }

    @Override
    public List<OutboxEntry> findDispatchable(int limit, Instant now) {
        Page<OutboxData> page = new Page<>(1, limit, false);
        IPage<OutboxData> result = mapper.selectPage(page,
                dispatchableCandidatesQuery(now).orderByAsc(OutboxData::getOccurredAt));
        return result.getRecords().stream().map(OutboxData::toEntry).toList();
    }

    /// "可派发候选" 的 WHERE 条件：{@code status IN (PENDING, FAILED) AND retry-due}。
    /// {@link #findDispatchable} 与 {@link #claimDispatchable} 共用此条件，各自指定 orderBy。
    /// <p>
    /// retry-due 语义：{@code nextRetryAt IS NULL}（从未失败过）OR {@code nextRetryAt ≤ now}（已到重试时间）。
    private static LambdaQueryWrapper<OutboxData> dispatchableCandidatesQuery(Instant now) {
        return Wrappers.lambdaQuery(OutboxData.class)
                .in(OutboxData::getStatus,
                        OutboxStatus.PENDING.name(),
                        OutboxStatus.FAILED.name())
                .and(wrapper -> wrapper
                        .isNull(OutboxData::getNextRetryAt)
                        .or()
                        .le(OutboxData::getNextRetryAt, now));
    }

    @Override
    public void markAsPublished(String eventId) {
        OutboxData data = mapper.selectById(eventId);
        if (data == null) {
            return;
        }
        OutboxEntry entry = OutboxData.toEntry(data);
        entry.markPublished();
        mapper.updateById(OutboxData.fromEntry(entry));
    }

    @Override
    public void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff) {
        OutboxData data = mapper.selectById(eventId);
        if (data == null) {
            return;
        }
        OutboxEntry entry = OutboxData.toEntry(data);
        entry.markFailed(errorMessage, maxRetries, backoff);
        mapper.updateById(OutboxData.fromEntry(entry));
    }

    @Override
    public void reactivate(String eventId) {
        OutboxData data = mapper.selectById(eventId);
        if (data == null) {
            return;
        }
        OutboxEntry entry = OutboxData.toEntry(data);
        entry.reactivate();
        mapper.updateById(OutboxData.fromEntry(entry));
    }

    /// CAS 两步法 claim：候选集 selectPage + 逐条 CAS UPDATE，外层 retry 直到拿满 limit 或候选耗尽。
    /// <p>
    /// 候选集语义与 {@link #findDispatchable} 一致：PENDING 全部 + FAILED 已到 {@code next_retry_at}。
    /// CAS 守卫 {@code WHERE event_id=? AND status=candidate.status} 防止并发 claimer 抢同一行；
    /// CAS 失败（{@code affectedRows=0}）说明该行已被其他 claimer 抢走（status 已变 DISPATCHING），
    /// 跳过该行。所有 CAS 失败的行已被他人改成 DISPATCHING，下一轮 selectPage 天然过滤掉；
    /// 因此外层 while 循环最终一定收敛——要么拿满 limit，要么 selectPage 返回空。
    /// <p>
    /// 调用方契约：返回值 ≤ {@code limit}。候选池不足时返回 < limit（dispatcher 下一轮再 dispatch）。
    @Override
    public List<OutboxEntry> claimDispatchable(int limit, String claimerId) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        if (claimerId == null || claimerId.isBlank()) {
            throw new IllegalArgumentException("claimerId must not be blank");
        }
        // claimToken 保留：P3-2 语义对齐（运维可通过 token 关联本批 SQL），但 CAS 模式下
        // 不再依赖 token 做回读去重——CAS 失败的行天然不会出现在返回值里。
        String claimToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        List<OutboxEntry> claimed = new ArrayList<>(limit);
        while (claimed.size() < limit) {
            int remaining = limit - claimed.size();
            // step 1: 候选集 selectPage（LIMIT 由 PaginationInnerInterceptor 按方言生成）。
            // orderBy eventId 让多 pod 并发 CAS 时有确定顺序，降低死锁概率。
            Page<OutboxData> page = new Page<>(1, remaining, false);
            IPage<OutboxData> result = mapper.selectPage(page,
                    dispatchableCandidatesQuery(now).orderByAsc(OutboxData::getEventId));
            if (result.getRecords().isEmpty()) {
                break;
            }
            // step 2: 逐条 CAS UPDATE。WHERE status=candidate.status 是乐观锁守卫。
            for (OutboxData candidate : result.getRecords()) {
                if (claimed.size() >= limit) {
                    break;
                }
                Instant claimTime = Instant.now();
                int updated = mapper.update(null,
                        Wrappers.lambdaUpdate(OutboxData.class)
                                .set(OutboxData::getStatus, OutboxStatus.DISPATCHING.name())
                                .set(OutboxData::getClaimedBy, claimerId)
                                .set(OutboxData::getClaimToken, claimToken)
                                .set(OutboxData::getClaimedAt, claimTime)
                                .eq(OutboxData::getEventId, candidate.getEventId())
                                .eq(OutboxData::getStatus, candidate.getStatus()));
                if (updated == 1) {
                    // 返回值反映 claim 后的状态（DB 已写入，candidate 内存副本同步）。
                    candidate.setStatus(OutboxStatus.DISPATCHING.name());
                    candidate.setClaimedBy(claimerId);
                    candidate.setClaimToken(claimToken);
                    candidate.setClaimedAt(claimTime);
                    claimed.add(OutboxData.toEntry(candidate));
                }
                // CAS 失败（updated=0）说明并发 claimer 已抢走该行，跳过；
                // 外层 while 会重新 selectPage 拿下一批候选补齐 limit。
            }
        }
        return claimed;
    }

    /// stuck-DISPATCHING 恢复：把 {@code claimed_at < cutoff} 的 DISPATCHING 记录回滚为 PENDING，
    /// 清空 {@code claimed_at / claimed_by / claim_token}。
    /// <p>
    /// 标准 {@code UPDATE...WHERE} 跨方言。pod 崩溃 / kill -9 后 DISPATCHING 中途残留的记录在此被回收，
    /// 下一个 dispatcher 周期会重新 claim。
    @Override
    public int recoverStuckDispatching(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff must not be null");
        }
        return mapper.update(null,
                Wrappers.lambdaUpdate(OutboxData.class)
                        .set(OutboxData::getStatus, OutboxStatus.PENDING.name())
                        .set(OutboxData::getClaimedAt, null)
                        .set(OutboxData::getClaimedBy, null)
                        .set(OutboxData::getClaimToken, null)
                        .eq(OutboxData::getStatus, OutboxStatus.DISPATCHING.name())
                        .lt(OutboxData::getClaimedAt, cutoff));
    }

    /// 批量清理：删除指定终态（PUBLISHED / DEAD_LETTERED）且 {@code occurred_at} 早于 {@code cutoff}
    /// 的记录，每批最多 {@code batchSize} 条，循环直到候选耗尽。
    /// <p>
    /// selectPage + removeByIds 两步法：selectPage 的 LIMIT 由 PaginationInnerInterceptor 按方言生成；
    /// removeByIds 按主键删除，是标准 ANSI SQL。不再使用 {@code DELETE...IN (SELECT...LIMIT)} 形态。
    /// <p>
    /// 循环而非单条 SQL 原因：单批 DELETE 拿锁较多且长事务影响 claim/dispatch；分批每批只锁 batchSize 行，
    /// 批次间释放锁，其它事务能穿插。{@code deleted < batchSize} 表示候选集已耗尽。
    @Override
    public int deleteByStatusAndCreatedAtBefore(OutboxStatus status, Instant cutoff, int batchSize) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff must not be null");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive: " + batchSize);
        }
        int total = 0;
        while (true) {
            Page<OutboxData> page = new Page<>(1, batchSize, false);
            IPage<OutboxData> batch = mapper.selectPage(page,
                    Wrappers.lambdaQuery(OutboxData.class)
                            .eq(OutboxData::getStatus, status.name())
                            .lt(OutboxData::getOccurredAt, cutoff)
                            .orderByAsc(OutboxData::getEventId));
            if (batch.getRecords().isEmpty()) {
                break;
            }
            List<String> ids = batch.getRecords().stream()
                    .map(OutboxData::getEventId)
                    .toList();
            int deleted = mapper.deleteByIds(ids);
            total += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        return total;
    }
}
