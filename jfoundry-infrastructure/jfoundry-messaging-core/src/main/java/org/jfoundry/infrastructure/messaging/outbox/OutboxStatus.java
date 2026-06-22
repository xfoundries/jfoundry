package org.jfoundry.infrastructure.messaging.outbox;

/// Outbox 条目的四种状态。
/// <p>
/// 状态流转：PENDING → PUBLISHED（成功终态）；PENDING/FAILED → FAILED（重试窗口未到）→
/// FAILED → DEAD_LETTERED（重试耗尽，死信终态）；DEAD_LETTERED → PENDING（reactivate）。
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD_LETTERED
}
