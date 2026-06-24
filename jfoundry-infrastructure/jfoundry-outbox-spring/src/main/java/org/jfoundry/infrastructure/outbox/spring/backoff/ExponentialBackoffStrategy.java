package org.jfoundry.infrastructure.outbox.spring.backoff;

import org.jfoundry.application.outbox.BackoffStrategy;

import java.time.Duration;

/// 指数退避策略：delay = min(base * 2^failedAttempts, max)。
/// <p>
/// 默认序列（base=1000ms, max=5min）：1s, 2s, 4s, 8s, 16s, 32s, 60s(撞顶), 60s, ...。
public record ExponentialBackoffStrategy(long baseMs, long maxMs) implements BackoffStrategy {

    public ExponentialBackoffStrategy {
        if (baseMs <= 0) {
            throw new IllegalArgumentException("baseMs 必须为正：" + baseMs);
        }
        if (maxMs < baseMs) {
            throw new IllegalArgumentException("maxMs 不能小于 baseMs：maxMs=" + maxMs + ", baseMs=" + baseMs);
        }
    }

    @Override
    public Duration nextDelay(int failedAttempts) {
        int exponent = Math.max(0, failedAttempts);
        long computed;
        try {
            computed = Math.multiplyExact(baseMs, 1L << exponent);
        } catch (ArithmeticException overflow) {
            computed = Long.MAX_VALUE;
        }
        long delayMs = Math.min(computed, maxMs);
        return Duration.ofMillis(delayMs);
    }
}
