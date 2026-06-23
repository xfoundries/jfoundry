package org.jfoundry.infrastructure.messaging.mybatis.outbox;

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
import java.util.List;
import java.util.UUID;

/// OutboxRepository 默认实现：基于 MyBatis-Plus。
/// <p>
/// SPI 层 {@link OutboxEntry} 不携带任何 ORM 注解，本类在边界处负责 entry ↔ {@link OutboxData}
/// 互转。findDispatchable 通过 selectPage 表达"取前 N 条"，由 PaginationInnerInterceptor
/// 自动生成对应方言的 SQL。markAsPublished / markAsFailed / reactivate 采用 read-then-update
/// 模式，状态流转由 OutboxEntry 自身封装。
/// <p>
/// 多实例安全性：claim 主链路走 {@link #claimDispatchable(int, String)} —— 单语句原子
/// {@code UPDATE...LIMIT N}（MySQL/H2）对候选行加排他锁，两个并发 pod 会被锁串行化，
/// 自动选取不同行，无需应用层 retry。回读使用每次调用现生成的 {@code claimToken}，避免
/// 按稳定 podId 回读时把前一批未完成状态更新的 DISPATCHING 旧记录一起带走（P3-2 修复）。
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
                            + "OutboxRepository.findDispatchable 依赖分页插件生成方言 SQL，"
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
                Wrappers.lambdaQuery(OutboxData.class)
                        .in(OutboxData::getStatus,
                                OutboxStatus.PENDING.name(),
                                OutboxStatus.FAILED.name())
                        .and(wrapper -> wrapper
                                .isNull(OutboxData::getNextRetryAt)
                                .or()
                                .le(OutboxData::getNextRetryAt, now))
                        .orderByAsc(OutboxData::getOccurredAt));
        return result.getRecords().stream().map(OutboxData::toEntry).toList();
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

    @Override
    public List<OutboxEntry> claimDispatchable(int limit, String claimerId) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        if (claimerId == null || claimerId.isBlank()) {
            throw new IllegalArgumentException("claimerId must not be blank");
        }
        // P3-2: 每次调用现生成 UUID 作为 claimToken，写入 claim_token 列。
        // 回读按 token 精确匹配，避开"按稳定 podId 回读时把前一批未完成状态更新的
        // DISPATCHING 旧记录一起带走"的重复发送根因。
        String claimToken = UUID.randomUUID().toString();
        // Default to MySQL/H2 dialect; dialect dispatch added in Task 2.5.
        // UPDATE...ORDER BY event_id LIMIT N 在 MySQL/H2 下是原子的：UPDATE 执行时对匹配行
        // 加排他锁，两个并发 UPDATE 会被锁串行化，后执行者看到的 PENDING 集合已不包含前者
        // claim 走的行，自动选取不同行。
        mapper.claimPending(limit, claimerId, claimToken);
        return mapper.selectByClaimToken(claimToken).stream()
                .map(OutboxData::toEntry)
                .toList();
    }

    @Override
    public int recoverStuckDispatching(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff must not be null");
        }
        // Single-row-lock UPDATE across multiple rows, portable across MySQL / H2 / DM.
        // Pod 崩溃 / kill -9 后 DISPATCHING 中途残留的记录在此被回滚为 PENDING，
        // 下一个 dispatcher 周期会重新 claim。
        return mapper.resetStuckDispatching(cutoff);
    }

    /// P2-5 cleanup: 循环调用 mapper 的 batch delete，直到返回 &lt; batchSize，保证把所有
    /// 匹配的终态记录删干净。
    /// <p>
    /// 循环而非单条 SQL 原因：单批 DELETE 拿锁较多且长事务影响 claim/dispatch；
    /// 分批每批只锁 batchSize 行，批次间释放锁，其它事务能穿插。{@code deleted == batchSize}
    /// 判定表示可能还有剩余记录，继续下一批；{@code deleted < batchSize} 表示候选集已耗尽。
    /// <p>
    /// 候选集快照问题：每批 DELETE 的子查询是该批语句开始时的快照，但 DELETE 会对匹配行
    /// 加排他锁，与并发 claim/dispatch 互不干扰——cleanup 目标是终态记录，不会被主链路改写。
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
        int deleted;
        do {
            deleted = mapper.deleteBatchByStatusAndOccurredBefore(status.name(), cutoff, batchSize);
            total += deleted;
        } while (deleted == batchSize);
        return total;
    }
}
