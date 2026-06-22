package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.autoconfigure.persistence.OutboxMybatisPlusAutoConfiguration;
import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.outbox.BackoffStrategy;
import org.jfoundry.infrastructure.messaging.outbox.OutboxDispatcher;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.spring.backoff.ExponentialBackoffStrategy;
import org.jfoundry.infrastructure.messaging.spring.dispatcher.ScheduledOutboxDispatcher;
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
/// 根据 {@code ddd.outbox.dispatcher.mode} 选择 Dispatcher 实现：
/// <ul>
///   <li>{@code scheduled}（默认）：注册 ScheduledOutboxDispatcher（本类已加 @EnableScheduling）。</li>
///   <li>{@code jobrunr}：要求 classpath 有 ddd-messaging-jobrunr，由业务侧或该模块自身的 AutoConfiguration 注册。</li>
/// </ul>
/// {@code ddd.outbox.dispatcher.enabled=false} 是信息性开关：本配置不会因此自动跳过 bean 注册，
/// 业务侧需要禁用调度时，自行关闭 {@code @EnableScheduling} 或在业务侧 {@code @Configuration} 上排除本 AutoConfiguration。
@AutoConfiguration
@AutoConfigureAfter({
        MessageSenderAutoConfiguration.class,
        OutboxMybatisPlusAutoConfiguration.class
})
@EnableConfigurationProperties(OutboxDispatcherProperties.class)
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
}
