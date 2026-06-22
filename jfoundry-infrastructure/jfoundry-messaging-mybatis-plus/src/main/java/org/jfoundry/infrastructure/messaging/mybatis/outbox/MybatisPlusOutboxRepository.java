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

/// OutboxRepository 默认实现：基于 MyBatis-Plus。
/// <p>
/// SPI 层 {@link OutboxEntry} 不携带任何 ORM 注解，本类在边界处负责 entry ↔ {@link OutboxData}
/// 互转。findDispatchable 通过 selectPage 表达"取前 N 条"，由 PaginationInnerInterceptor
/// 自动生成对应方言的 SQL。markAsPublished / markAsFailed / reactivate 采用 read-then-update
/// 模式，状态流转由 OutboxEntry 自身封装。非原子操作；多实例安全性由消费端幂等保证（v1 接受）。
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
        // Default to MySQL/H2 dialect; dialect dispatch added in Task 2.5.
        // UPDATE...ORDER BY event_id LIMIT N 在 MySQL/H2 下是原子的：UPDATE 执行时对匹配行
        // 加排他锁，两个并发 UPDATE 会被锁串行化，后执行者看到的 PENDING 集合已不包含前者
        // claim 走的行，自动选取不同行。
        mapper.claimPending(limit, claimerId);
        return mapper.selectByClaimer(claimerId).stream()
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
}
