package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

/// 周期性恢复卡住的 DISPATCHING 记录。
/// <p>
/// 场景：pod 在 DISPATCHING 中途崩溃 / kill -9，记录残留在 DISPATCHING 状态。
/// 本任务按 {@code jfoundry.outbox.recovery.stuck-timeout} 阈值回滚。
/// <p>
/// 调度：{@code @Scheduled(fixedDelayString = "${jfoundry.outbox.recovery.interval:60000}")}，
/// 默认 60s（60000ms）。{@code jfoundry.outbox.recovery.interval} 接受 Spring Boot 的
/// Duration 字符串（如 {@code 60s}/{@code PT1M}/{@code 60000}），由
/// {@link OutboxRecoveryProperties#getInterval()} 解析；placeholder 退出时必须以毫秒呈现。
public class OutboxRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryJob.class);

    private final OutboxRepository outboxRepository;
    private final OutboxRecoveryProperties properties;

    @Autowired
    public OutboxRecoveryJob(OutboxRepository outboxRepository, OutboxRecoveryProperties properties) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
    }

    /// 重置卡住的 DISPATCHING 记录。返回回滚的记录数，便于测试断言与运维监控。
    @Scheduled(fixedDelayString = "${jfoundry.outbox.recovery.interval:60000}")
    public int recoverStuckDispatching() {
        Duration timeout = properties.getStuckTimeout();
        Instant cutoff = Instant.now().minus(timeout);
        int recovered = outboxRepository.recoverStuckDispatching(cutoff);
        if (recovered > 0) {
            log.warn("Recovered {} stuck DISPATCHING outbox records (threshold={})",
                    recovered, timeout);
        }
        return recovered;
    }
}
