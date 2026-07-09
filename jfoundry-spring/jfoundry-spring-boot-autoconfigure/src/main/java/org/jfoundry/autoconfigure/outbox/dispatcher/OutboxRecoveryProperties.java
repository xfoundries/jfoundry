package org.jfoundry.autoconfigure.outbox.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Outbox DISPATCHING recovery job configuration.
/// <p>
/// Prefix: {@code jfoundry.outbox.recovery}
/// <p>
/// Scenario: a pod may crash or be killed while rows are in DISPATCHING, leaving records stuck in
/// that state. {@link OutboxRecoveryJob} periodically rolls them back according to
/// {@link #stuckTimeout}.
@ConfigurationProperties(prefix = "jfoundry.outbox.recovery")
public class OutboxRecoveryProperties {

    /// Whether the recovery job is enabled.
    /// <p>
    /// When unset, recovery follows dispatcher mode: enabled for {@code scheduled} and
    /// {@code jobrunr}, disabled for {@code none}. Set this property to {@code false} to disable
    /// recovery in {@code scheduled} or {@code jobrunr} mode.
    private Boolean enabled;

    /// Recovery job interval. Defaults to 60s.
    /// <p>
    /// Binds {@code jfoundry.outbox.recovery.interval}, which is read by {@code @Scheduled}.
    private Duration interval = Duration.ofSeconds(60);

    /// DISPATCHING stuck threshold. Defaults to 5min.
    /// <p>
    /// Records with claimedAt earlier than {@code now - stuckTimeout} are rolled back to PENDING.
    private Duration stuckTimeout = Duration.ofMinutes(5);

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public Duration getStuckTimeout() { return stuckTimeout; }
    public void setStuckTimeout(Duration stuckTimeout) { this.stuckTimeout = stuckTimeout; }
}
