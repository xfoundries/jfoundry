package org.jfoundry.infrastructure.messaging.spring.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Outbox Dispatcher 配置项。
/// <p>
/// 绑定 {@code jfoundry.outbox.dispatcher.*} 前缀。本类位于 jfoundry-messaging-spring，
/// 让 jfoundry-autoconfigure 和 jfoundry-messaging-jobrunr 都能引用同一份配置（避免循环依赖：
/// jobrunr 已经依赖 messaging-spring，但反过来不能依赖 autoconfigure）。
@ConfigurationProperties(prefix = "jfoundry.outbox.dispatcher")
public class OutboxDispatcherProperties {

    private boolean enabled = true;
    private String mode = "scheduled";
    private long intervalMs = 5000;
    private String cron = "*/10 * * * * *";
    private int batchSize = 50;
    private int maxRetries = 5;
    private long backoffBaseMs = 1000;
    private long backoffMaxMs = 300_000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
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
