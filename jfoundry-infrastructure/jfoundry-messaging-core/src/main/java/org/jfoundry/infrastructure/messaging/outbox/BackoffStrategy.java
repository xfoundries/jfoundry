package org.jfoundry.infrastructure.messaging.outbox;

import java.time.Duration;

/// 重试退避策略抽象。
/// <p>
/// 入参 failedAttempts 为 0-based "已失败次数"：
/// <ul>
///   <li>第 1 次失败（调用方传入 0）→ delay = base</li>
///   <li>第 2 次失败（调用方传入 1）→ delay = base * 2</li>
/// </ul>
public interface BackoffStrategy {

    /// @param failedAttempts 已失败次数（0-based）
    /// @return 下次重试前的等待时长
    Duration nextDelay(int failedAttempts);
}
