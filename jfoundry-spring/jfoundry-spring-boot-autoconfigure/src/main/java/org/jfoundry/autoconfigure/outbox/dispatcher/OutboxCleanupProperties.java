package org.jfoundry.autoconfigure.outbox.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// P2-5: Outbox cleanup job configuration.
/// <p>
/// Prefix: {@code jfoundry.outbox.cleanup}
/// <p>
/// Scenario: accumulated PUBLISHED / DEAD_LETTERED terminal records in the Outbox table can slow
/// down claim/dispatch queries. {@link OutboxCleanupJob} periodically removes expired terminal
/// records according to {@link #publishedRetentionDays} and {@link #deadLetteredRetentionDays}.
/// <p>
/// Defaults:
/// <ul>
///   <li>{@link #interval} = 24h, once per day.</li>
///   <li>{@link #publishedRetentionDays} = 7, keeping PUBLISHED records for 7 days.</li>
///   <li>{@link #deadLetteredRetentionDays} = 30, keeping DEAD_LETTERED records for 30 days for
///       investigation.</li>
///   <li>{@link #batchSize} = 1000, deleting at most 1000 rows per batch and looping until exhausted.</li>
/// </ul>
@ConfigurationProperties(prefix = "jfoundry.outbox.cleanup")
public class OutboxCleanupProperties {

    /// Cleanup job interval. Defaults to 24h.
    /// <p>
    /// Binds {@code jfoundry.outbox.cleanup.interval}, which is read by {@code @Scheduled}.
    /// The resolved {@code @Scheduled(fixedDelayString)} placeholder must be a millisecond literal,
    /// so {@link OutboxCleanupJob} uses {@code 86400000} as the placeholder default. YAML can still
    /// use Duration strings such as {@code 24h}, {@code PT24H}, or {@code 86400000}; Spring Boot
    /// {@code DurationStyle} performs the conversion.
    private Duration interval = Duration.ofHours(24);

    /// PUBLISHED record retention in days. Defaults to 7 days.
    /// <p>
    /// PUBLISHED records with occurredAt earlier than {@code now - publishedRetentionDays} are deleted.
    private int publishedRetentionDays = 7;

    /// DEAD_LETTERED record retention in days. Defaults to 30 days.
    /// <p>
    /// DEAD_LETTERED records with occurredAt earlier than {@code now - deadLetteredRetentionDays}
    /// are deleted. This retention is longer than PUBLISHED retention to support dead-letter
    /// investigation and manual reactivation.
    private int deadLetteredRetentionDays = 30;

    /// Maximum records deleted per batch. Defaults to 1000.
    /// <p>
    /// The repository layer loops over mapper batch deletes until the returned count is
    /// &lt; batchSize. Smaller batches reduce lock duration per transaction and avoid slowing the
    /// main claim/dispatch path.
    private int batchSize = 1000;

    /// Whether the cleanup job is enabled.
    /// <p>
    /// When unset, cleanup follows dispatcher mode: enabled for {@code scheduled} and
    /// {@code jobrunr}, disabled for {@code none}. Set this property to {@code false} to disable
    /// cleanup in {@code scheduled} or {@code jobrunr} mode.
    private Boolean enabled;

    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public int getPublishedRetentionDays() { return publishedRetentionDays; }
    public void setPublishedRetentionDays(int publishedRetentionDays) { this.publishedRetentionDays = publishedRetentionDays; }
    public int getDeadLetteredRetentionDays() { return deadLetteredRetentionDays; }
    public void setDeadLetteredRetentionDays(int deadLetteredRetentionDays) { this.deadLetteredRetentionDays = deadLetteredRetentionDays; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
