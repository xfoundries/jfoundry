package org.jfoundry.infrastructure.outbox.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.jfoundry.application.outbox.OutboxEntry;
import org.jfoundry.application.outbox.OutboxStatus;

import java.time.Instant;

/// Outbox 表 MyBatis-Plus 持久化数据对象。
/// <p>
/// 与 SPI 层的 {@link OutboxEntry} 字段一一对应，但携带 MyBatis-Plus 注解
/// （{@code @TableName("jfoundry_outbox_event")} + {@code @TableId(type = IdType.INPUT)}）。
/// SPI 层不绑定任何 ORM；本类把 Outbox 字段固定为 MyBatis-Plus 的实体视图，
/// 由 {@link MybatisPlusOutboxRepository} 负责 entry ↔ data 互转。
@TableName("jfoundry_outbox_event")
public class OutboxData {

    @TableId(type = IdType.INPUT)
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
    /// P2-1: atomic claim columns — 与 OutboxEntry.claimedAt 对齐，由 Task 2.1 schema 增补。
    private Instant claimedAt;
    /// P2-1: claim 该条目的 pod 标识 — 与 OutboxEntry.claimedBy 对齐。
    private String claimedBy;
    /// P3-2: 本次 claimDispatchable 调用生成的唯一 token — 与 OutboxEntry.claimToken 对齐。
    private String claimToken;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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

    /// SPI entry → MP data。
    /// <p>
    /// 状态字符串以 raw 形式复制（保留 OutboxStatus.name() 字面量），由 SPI entry 自身维护一致性。
    public static OutboxData fromEntry(OutboxEntry entry) {
        OutboxData data = new OutboxData();
        data.eventId = entry.getEventId();
        data.topic = entry.getTopic();
        data.payloadKey = entry.getPayloadKey();
        data.payloadType = entry.getPayloadType();
        data.payloadJson = entry.getPayloadJson();
        data.aggregateType = entry.getAggregateType();
        data.aggregateId = entry.getAggregateId();
        data.aggregateVersion = entry.getAggregateVersion();
        OutboxStatus status = entry.getStatus();
        data.status = status != null ? status.name() : null;
        data.retryCount = entry.getRetryCount();
        data.errorMessage = entry.getErrorMessage();
        data.occurredAt = entry.getOccurredAt();
        data.lastAttemptAt = entry.getLastAttemptAt();
        data.nextRetryAt = entry.getNextRetryAt();
        data.createdAt = entry.getCreatedAt();
        data.updatedAt = entry.getUpdatedAt();
        data.claimedAt = entry.getClaimedAt();
        data.claimedBy = entry.getClaimedBy();
        data.claimToken = entry.getClaimToken();
        return data;
    }

    /// MP data → SPI entry。
    public static OutboxEntry toEntry(OutboxData data) {
        OutboxEntry entry = new OutboxEntry();
        entry.setEventId(data.eventId);
        entry.setTopic(data.topic);
        entry.setPayloadKey(data.payloadKey);
        entry.setPayloadType(data.payloadType);
        entry.setPayloadJson(data.payloadJson);
        entry.setAggregateType(data.aggregateType);
        entry.setAggregateId(data.aggregateId);
        entry.setAggregateVersion(data.aggregateVersion);
        if (data.status != null) {
            entry.setStatus(OutboxStatus.valueOf(data.status));
        }
        entry.setRetryCount(data.retryCount);
        entry.setErrorMessage(data.errorMessage);
        entry.setOccurredAt(data.occurredAt);
        entry.setLastAttemptAt(data.lastAttemptAt);
        entry.setNextRetryAt(data.nextRetryAt);
        entry.setCreatedAt(data.createdAt);
        entry.setUpdatedAt(data.updatedAt);
        entry.setClaimedAt(data.claimedAt);
        entry.setClaimedBy(data.claimedBy);
        entry.setClaimToken(data.claimToken);
        return entry;
    }
}
