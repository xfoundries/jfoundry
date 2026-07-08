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

    /// Recovery job interval. Defaults to 60s.
    /// <p>
    /// Binds {@code jfoundry.outbox.recovery.interval}, which is read by {@code @Scheduled}.
    private Duration interval = Duration.ofSeconds(60);

    /// DISPATCHING stuck threshold. Defaults to 5min.
    /// <p>
    /// Records with claimedAt earlier than {@code now - stuckTimeout} are rolled back to PENDING.
    private Duration stuckTimeout = Duration.ofMinutes(5);

    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public Duration getStuckTimeout() { return stuckTimeout; }
    public void setStuckTimeout(Duration stuckTimeout) { this.stuckTimeout = stuckTimeout; }
}
