package org.jfoundry.autoconfigure.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// P2-5: Outbox 清理任务配置。
/// <p>
/// Prefix: {@code jfoundry.outbox.cleanup}
/// <p>
/// 场景：Outbox 表中 PUBLISHED / DEAD_LETTERED 终态记录堆积会拖慢 claim/dispatch 查询，
/// {@link OutboxCleanupJob} 按 {@link #publishedRetentionDays} /
/// {@link #deadLetteredRetentionDays} 周期性清理过期终态记录。
/// <p>
/// 默认值：
/// <ul>
///   <li>{@link #interval} = 24h（每日执行一次）</li>
///   <li>{@link #publishedRetentionDays} = 7（PUBLISHED 记录保留 7 天）</li>
///   <li>{@link #deadLetteredRetentionDays} = 30（DEAD_LETTERED 记录保留 30 天，便于排查）</li>
///   <li>{@link #batchSize} = 1000（每批最多删 1000 条，循环到删干净）</li>
/// </ul>
@ConfigurationProperties(prefix = "jfoundry.outbox.cleanup")
public class OutboxCleanupProperties {

    /// 清理任务执行间隔。默认 24h。
    /// <p>
    /// 绑定 {@code jfoundry.outbox.cleanup.interval}（{@code @Scheduled} 读取）。
    /// {@code @Scheduled(fixedDelayString)} 的 placeholder 退出时必须是毫秒字面量，
    /// 因此 {@link OutboxCleanupJob} 上的 placeholder 默认值是 {@code 86400000}；
    /// YAML 侧仍可使用 Duration 字符串（{@code 24h}/{@code PT24H}/{@code 86400000}），
    /// 由 Spring Boot {@code DurationStyle} 统一转换。
    private Duration interval = Duration.ofHours(24);

    /// PUBLISHED 记录保留天数。默认 7 天。
    /// <p>
    /// occurredAt 早于 {@code now - publishedRetentionDays} 的 PUBLISHED 记录被删除。
    private int publishedRetentionDays = 7;

    /// DEAD_LETTERED 记录保留天数。默认 30 天。
    /// <p>
    /// occurredAt 早于 {@code now - deadLetteredRetentionDays} 的 DEAD_LETTERED 记录被删除。
    /// 比 PUBLISHED 长，便于死信排查与人工 reactivate。
    private int deadLetteredRetentionDays = 30;

    /// 单批删除的最多记录数。默认 1000。
    /// <p>
    /// Repository 层循环调用 mapper 的 batch delete，直到返回 &lt; batchSize。
    /// 小批次可以减少单次事务持锁时长，避免拖慢主链路的 claim/dispatch。
    private int batchSize = 1000;

    /// 是否启用清理任务。默认 {@code true}。
    /// <p>
    /// 注意：与 {@code jfoundry.outbox.dispatcher.enabled} 解耦——即使 dispatcher 被禁用，
    /// cleanup 仍可独立工作（只要 OutboxRepository bean 存在）。本开关只控制
    /// {@link OutboxCleanupJob#runOnce()} 是否实际执行删除；Spring 调度仍会按
    /// {@link #interval} 周期触发，但 {@code runOnce} 会直接返回 0。
    private boolean enabled = true;

    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public int getPublishedRetentionDays() { return publishedRetentionDays; }
    public void setPublishedRetentionDays(int publishedRetentionDays) { this.publishedRetentionDays = publishedRetentionDays; }
    public int getDeadLetteredRetentionDays() { return deadLetteredRetentionDays; }
    public void setDeadLetteredRetentionDays(int deadLetteredRetentionDays) { this.deadLetteredRetentionDays = deadLetteredRetentionDays; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
