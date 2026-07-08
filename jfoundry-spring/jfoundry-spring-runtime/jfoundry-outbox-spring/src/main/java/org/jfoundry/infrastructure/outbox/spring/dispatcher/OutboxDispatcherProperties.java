package org.jfoundry.infrastructure.outbox.spring.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Outbox Dispatcher configuration properties.
/// <p>
/// Binds the {@code jfoundry.outbox.dispatcher.*} prefix. This class lives in
/// jfoundry-outbox-spring so jfoundry-autoconfigure and jfoundry-outbox-jobrunr can reference the
/// same configuration without creating a cycle: jobrunr already depends on outbox-spring, but
/// outbox-spring must not depend back on autoconfigure.
@ConfigurationProperties(prefix = "jfoundry.outbox.dispatcher")
public class OutboxDispatcherProperties {

    private String mode = "scheduled";
    private long intervalMs = 5000;
    private String cron = "*/10 * * * * *";
    private int batchSize = 50;
    private int maxRetries = 5;
    private long backoffBaseMs = 1000;
    private long backoffMaxMs = 300_000;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public long getIntervalMs() { return intervalMs; }
    public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getBackoffBaseMs() { return backoffBaseMs; }
    public void setBackoffBaseMs(long backoffBaseMs) { this.backoffBaseMs = backoffBaseMs; }
    public long getBackoffMaxMs() { return backoffMaxMs; }
    public void setBackoffMaxMs(long backoffMaxMs) { this.backoffMaxMs = backoffMaxMs; }
}
