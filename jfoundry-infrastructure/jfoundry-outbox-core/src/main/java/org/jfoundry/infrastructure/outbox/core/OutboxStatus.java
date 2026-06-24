package org.jfoundry.infrastructure.outbox.core;

/// Outbox 条目状态。
/// <p>
/// 状态流转：
/// <ul>
///   <li>{@code PENDING} → {@code DISPATCHING}（原子 claim，多实例下互斥）→ {@code PUBLISHED}（成功终态）</li>
///   <li>{@code DISPATCHING} → {@code FAILED}（派发失败）→ {@code DEAD_LETTERED}（重试耗尽，死信终态）</li>
///   <li>{@code DISPATCHING} stuck → {@code PENDING}（recovery 任务回滚，见 P2-1）</li>
///   <li>{@code DEAD_LETTERED} → {@code PENDING}（reactivate）</li>
/// </ul>
public enum OutboxStatus {
    PENDING,
    DISPATCHING,
    PUBLISHED,
    FAILED,
    DEAD_LETTERED
}
