package org.jfoundry.application.outbox;

import java.time.Instant;

/// Outbox 表 SPI 数据对象 + 状态机方法。
/// <p>
/// 字段对应表 {@code jfoundry_outbox_event}，但本类不携带任何 ORM 注解 —— 具体的表名/主键策略
/// 由各持久化实现自行建模（例如 jfoundry-persistence-mybatis-plus 模块的 {@code OutboxData}）。
/// <p>
/// 状态流转（PENDING / DISPATCHING / FAILED / PUBLISHED / DEAD_LETTERED）由
/// {@link #markPublished()} / {@link #markFailed(String, int, BackoffStrategy)} /
/// {@link #reactivate()} 封装；{@code DISPATCHING} 状态的进入/退出由 P2-1 的原子 claim
/// 与 recovery 任务负责（见 {@code claimDispatchable}）。
public class OutboxMessage {

    private String eventId;
    private String topic;
    private String payloadKey;
    private String payloadType;
    private String payloadJson;
    private String aggregateType;
    private String aggregateId;
    private Long aggregateVersion;
    private String status;
    private int retryCount;
    private String errorMessage;
    private Instant occurredAt;
    private Instant lastAttemptAt;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant updatedAt;
    /// P2-1: 最近一次成功原子 claim 的时间，配合 {@code idx_outbox_claim (status, claimed_at)}
    /// 用于 DISPATCHING stuck 检测与回滚。
    private Instant claimedAt;
    /// P2-1: claim 该条目的 pod 标识（hostname + 短 UUID），用于诊断与多实例互斥。
    private String claimedBy;
    /// P3-2: 本次 claimDispatchable 调用生成的唯一 token（UUID）。
    /// <p>
    /// 回读时按 token 精确匹配本批 claim 的条目，避免按稳定 podId 回读时把前一批
    /// 因状态更新失败而残留的 DISPATCHING 旧记录一起带走（重复发送的根因）。
    /// 离开 DISPATCHING 状态时清空。
    private String claimToken;

    public static OutboxMessage newPending(String eventId, String topic, String payloadKey,
                                          String payloadType, String payloadJson, Instant occurredAt) {
        OutboxMessage entry = new OutboxMessage();
        Instant now = Instant.now();
        entry.eventId = eventId;
        entry.topic = topic;
        entry.payloadKey = payloadKey;
        entry.payloadType = payloadType;
        entry.payloadJson = payloadJson;
        entry.status = OutboxMessageStatus.PENDING.name();
        entry.retryCount = 0;
        entry.errorMessage = null;
        entry.occurredAt = occurredAt;
        entry.lastAttemptAt = null;
        entry.nextRetryAt = null;
        entry.createdAt = now;
        entry.updatedAt = now;
        return entry;
    }

    public static OutboxMessage newPending(String eventId, String topic, String payloadKey,
                                          String payloadType, String payloadJson, Instant occurredAt,
                                          String aggregateType, String aggregateId, Long aggregateVersion) {
        OutboxMessage entry = newPending(eventId, topic, payloadKey, payloadType, payloadJson, occurredAt);
        entry.aggregateType = aggregateType;
        entry.aggregateId = aggregateId;
        entry.aggregateVersion = aggregateVersion;
        return entry;
    }

    public void markPublished() {
        Instant now = Instant.now();
        this.status = OutboxMessageStatus.PUBLISHED.name();
        this.lastAttemptAt = now;
        // Claim 结束：本条记录不再被任何 pod 持有，清空 claim 元数据。
        this.claimedAt = null;
        this.claimedBy = null;
        this.claimToken = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, int maxRetries, BackoffStrategy backoff) {
        int retryCountBefore = this.retryCount;
        Instant now = Instant.now();
        this.lastAttemptAt = now;
        this.errorMessage = errorMessage;
        java.time.Duration delay = backoff.nextDelay(retryCountBefore);
        this.retryCount = retryCountBefore + 1;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxMessageStatus.DEAD_LETTERED.name();
            this.nextRetryAt = null;
        } else {
            this.status = OutboxMessageStatus.FAILED.name();
            this.nextRetryAt = now.plus(delay);
        }
        // Claim 结束（DISPATCHING → FAILED / DEAD_LETTERED）：本条记录不再被任何 pod 持有，
        // 清空 claim 元数据；下一次 retry 时由本 pod 或其它 pod 重新 claim。
        this.claimedAt = null;
        this.claimedBy = null;
        this.claimToken = null;
        this.updatedAt = now;
    }

    public void reactivate() {
        if (!OutboxMessageStatus.DEAD_LETTERED.name().equals(this.status)) {
            throw new IllegalStateException(
                    "reactivate 仅允许从 DEAD_LETTERED 状态转入 PENDING，当前状态: " + this.status);
        }
        Instant now = Instant.now();
        this.status = OutboxMessageStatus.PENDING.name();
        this.retryCount = 0;
        this.nextRetryAt = now;
        this.errorMessage = null;
        // Defensive：DEAD_LETTERED 已无 claim 持有者，但保证字段一致。
        this.claimedAt = null;
        this.claimedBy = null;
        this.claimToken = null;
        this.updatedAt = now;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getPayloadKey() { return payloadKey; }
    public void setPayloadKey(String payloadKey) { this.payloadKey = payloadKey; }
    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public Long getAggregateVersion() { return aggregateVersion; }
    public void setAggregateVersion(Long aggregateVersion) { this.aggregateVersion = aggregateVersion; }
    public OutboxMessageStatus getStatus() { return OutboxMessageStatus.valueOf(status); }
    public void setStatus(OutboxMessageStatus status) { this.status = status.name(); }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }
    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
    public String getClaimToken() { return claimToken; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
}
