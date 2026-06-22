package org.jfoundry.autoconfigure.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Outbox DISPATCHING 恢复任务配置。
/// <p>
/// Prefix: {@code jfoundry.outbox.recovery}
/// <p>
/// 场景：pod 在 DISPATCHING 中途崩溃 / kill -9，记录残留在 DISPATCHING 状态。
/// {@link OutboxRecoveryJob} 按 {@link #stuckTimeout} 阈值周期性回滚。
@ConfigurationProperties(prefix = "jfoundry.outbox.recovery")
public class OutboxRecoveryProperties {

    /// 恢复任务执行间隔。默认 60s。
    /// <p>
    /// 绑定 {@code jfoundry.outbox.recovery.interval}（{@code @Scheduled} 读取）。
    private Duration interval = Duration.ofSeconds(60);

    /// DISPATCHING 卡住阈值。默认 5min。
    /// <p>
    /// claimedAt 早于 {@code now - stuckTimeout} 的记录被回滚为 PENDING。
    private Duration stuckTimeout = Duration.ofMinutes(5);

    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public Duration getStuckTimeout() { return stuckTimeout; }
    public void setStuckTimeout(Duration stuckTimeout) { this.stuckTimeout = stuckTimeout; }
}
