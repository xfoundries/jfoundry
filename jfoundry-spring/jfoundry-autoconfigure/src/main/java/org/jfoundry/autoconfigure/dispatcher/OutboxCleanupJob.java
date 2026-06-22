package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

/// P2-5: 周期性清理 Outbox 表中已进入终态（PUBLISHED / DEAD_LETTERED）且超过保留期的记录。
/// <p>
/// 场景：Outbox 表中 PUBLISHED / DEAD_LETTERED 记录堆积会拖慢 claim/dispatch 查询，
/// 本任务按 {@link OutboxCleanupProperties#getPublishedRetentionDays()} /
/// {@link OutboxCleanupProperties#getDeadLetteredRetentionDays()} 周期性清理。
/// <p>
/// 调度：{@code @Scheduled(fixedDelayString = "${jfoundry.outbox.cleanup.interval:86400000}")}，
/// 默认 24h（86400000ms）。{@code jfoundry.outbox.cleanup.interval} 接受 Spring Boot 的
/// Duration 字符串（如 {@code 24h}/{@code PT24H}/{@code 86400000}），由
/// {@link OutboxCleanupProperties#getInterval()} 解析；placeholder 退出时必须以毫秒呈现，
/// 否则 {@code @Scheduled} 的 {@code long} 解析会抛 {@link IllegalArgumentException}
/// （例如字面量 {@code 24h} 在 Spring 6.x 会被 {@code Long.parseLong("24h")} 拒绝）。
/// <p>
/// 任务是幂等的——重复执行无副作用；失败不影响 Outbox 主链路（claim/dispatch 不依赖
/// 已删除的终态记录）。
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxRepository outboxRepository;
    private final OutboxCleanupProperties properties;

    @Autowired
    public OutboxCleanupJob(OutboxRepository outboxRepository, OutboxCleanupProperties properties) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
    }

    /// 执行一次清理。返回本轮删除的记录总数（PUBLISHED + DEAD_LETTERED），便于测试断言与运维监控。
    /// <p>
    /// 当 {@link OutboxCleanupProperties#isEnabled()} 为 {@code false} 时直接返回 0，
    /// 不访问 Repository——支持业务侧通过配置 {@code jfoundry.outbox.cleanup.enabled=false}
    /// 完全关闭删除（例如运行只读副本时）。
    @Scheduled(fixedDelayString = "${jfoundry.outbox.cleanup.interval:86400000}")
    public int runOnce() {
        if (!properties.isEnabled()) {
            return 0;
        }

        Instant now = Instant.now();
        Instant publishedCutoff = now.minus(Duration.ofDays(properties.getPublishedRetentionDays()));
        Instant deadCutoff = now.minus(Duration.ofDays(properties.getDeadLetteredRetentionDays()));

        int publishedDeleted = outboxRepository.deleteByStatusAndCreatedAtBefore(
                OutboxStatus.PUBLISHED, publishedCutoff, properties.getBatchSize());
        int deadDeleted = outboxRepository.deleteByStatusAndCreatedAtBefore(
                OutboxStatus.DEAD_LETTERED, deadCutoff, properties.getBatchSize());

        int total = publishedDeleted + deadDeleted;
        if (total > 0) {
            log.info("Outbox cleanup: deleted {} PUBLISHED (retention {}d), {} DEAD_LETTERED (retention {}d)",
                    publishedDeleted, properties.getPublishedRetentionDays(),
                    deadDeleted, properties.getDeadLetteredRetentionDays());
        }
        return total;
    }
}
