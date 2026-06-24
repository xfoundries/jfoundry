package org.jfoundry.autoconfigure.outbox.dispatcher;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.OutboxDispatcher;
import org.jfoundry.application.outbox.OutboxRepository;
import org.jfoundry.infrastructure.outbox.spring.backoff.ExponentialBackoffStrategy;
import org.jfoundry.infrastructure.outbox.spring.dispatcher.OutboxDispatcherProperties;
import org.jfoundry.infrastructure.outbox.spring.dispatcher.ScheduledOutboxDispatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/// Outbox Dispatcher 自动配置。
/// <p>
/// 根据 {@code jfoundry.outbox.dispatcher.mode} 选择 Dispatcher 实现：
/// <ul>
///   <li>{@code scheduled}（默认）：注册 ScheduledOutboxDispatcher（本类已加 @EnableScheduling）。</li>
///   <li>{@code jobrunr}：要求 classpath 有 jfoundry-outbox-jobrunr。Spring Boot starter
///       通过本 auto-configuration 模块注册 {@code JobRunrDispatcherAutoConfiguration}，
///       与本类互斥：
///       两端都用 {@code @ConditionalOnMissingBean(OutboxDispatcher.class)} 守护，
///       且 mode 分别匹配 {@code scheduled} / {@code jobrunr}，不会同时命中。</li>
/// </ul>
/// <p>
/// 总开关：{@code jfoundry.outbox.dispatcher.enabled=false} 将关闭整个 AutoConfiguration，
/// 所有 Dispatcher / BackoffStrategy bean 都不会注册，{@code @EnableScheduling} 也不会生效。
/// 默认 {@code matchIfMissing=true} 保持向后兼容。
@AutoConfiguration
@AutoConfigureAfter({
        MessageSenderAutoConfiguration.class,
        OutboxMybatisPlusAutoConfiguration.class
})
@EnableConfigurationProperties({OutboxDispatcherProperties.class, OutboxRecoveryProperties.class, OutboxCleanupProperties.class})
@ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxDispatcherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BackoffStrategy.class)
    public BackoffStrategy exponentialBackoffStrategy(OutboxDispatcherProperties properties) {
        return new ExponentialBackoffStrategy(properties.getBackoffBaseMs(), properties.getBackoffMaxMs());
    }

    @Bean
    @ConditionalOnBean({OutboxRepository.class, MessageSender.class, BackoffStrategy.class})
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    @ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "mode", havingValue = "scheduled", matchIfMissing = true)
    public ScheduledOutboxDispatcher scheduledOutboxDispatcher(
            OutboxRepository outboxRepository,
            MessageSender messageSender,
            BackoffStrategy backoffStrategy,
            OutboxDispatcherProperties properties) {
        return new ScheduledOutboxDispatcher(outboxRepository, messageSender,
                properties.getMaxRetries(), backoffStrategy, properties.getBatchSize());
    }

    /// P2-1 stuck-DISPATCHING recovery job。
    /// <p>
    /// 仅在 {@link OutboxRepository} 存在时注册；与 dispatcher mode 解耦，
    /// 即使 {@code mode=jobrunr} 也能独立回收 stuck 记录。
    @Bean
    @ConditionalOnBean({OutboxRepository.class})
    @ConditionalOnMissingBean(OutboxRecoveryJob.class)
    public OutboxRecoveryJob outboxRecoveryJob(OutboxRepository outboxRepository,
                                               OutboxRecoveryProperties recoveryProperties) {
        return new OutboxRecoveryJob(outboxRepository, recoveryProperties);
    }

    /// P2-5 terminal-state cleanup job。
    /// <p>
    /// 仅在 {@link OutboxRepository} 存在时注册；与 dispatcher mode 解耦，
    /// 即使 {@code mode=jobrunr} 也能独立清理 PUBLISHED / DEAD_LETTERED 记录。
    /// 任务的启停通过 {@link OutboxCleanupProperties#isEnabled()} 控制
    /// （默认 {@code true}），无需重启 ApplicationContext。
    @Bean
    @ConditionalOnBean({OutboxRepository.class})
    @ConditionalOnMissingBean(OutboxCleanupJob.class)
    public OutboxCleanupJob outboxCleanupJob(OutboxRepository outboxRepository,
                                             OutboxCleanupProperties cleanupProperties) {
        return new OutboxCleanupJob(outboxRepository, cleanupProperties);
    }
}
