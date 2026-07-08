package org.jfoundry.autoconfigure.outbox.dispatcher;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.OutboxDispatcher;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.outbox.spring.backoff.ExponentialBackoffStrategy;
import org.jfoundry.infrastructure.outbox.spring.dispatcher.OutboxDispatcherProperties;
import org.jfoundry.infrastructure.outbox.spring.dispatcher.ScheduledOutboxDispatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/// Auto-configuration for the Outbox Dispatcher.
/// <p>
/// Selects the Dispatcher implementation according to {@code jfoundry.outbox.dispatcher.mode}:
/// <ul>
///   <li>{@code scheduled} (default): registers ScheduledOutboxDispatcher. This class enables
///       scheduling.</li>
///   <li>{@code jobrunr}: requires jfoundry-outbox-jobrunr on the classpath. The Spring Boot starter
///       registers {@code JobRunrDispatcherAutoConfiguration} through this auto-configuration module.
///       It is mutually exclusive with this class: both sides are guarded by
///       {@code @ConditionalOnMissingBean(OutboxDispatcher.class)}, and their modes match
///       {@code scheduled} and {@code jobrunr} respectively, so they cannot both apply.</li>
/// </ul>
@AutoConfiguration
@AutoConfigureAfter(
        value = MessageSenderAutoConfiguration.class,
        name = "org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration"
)
@ConditionalOnClass({OutboxMessageStore.class, MessageSender.class, ScheduledOutboxDispatcher.class})
@EnableConfigurationProperties({OutboxDispatcherProperties.class, OutboxRecoveryProperties.class, OutboxCleanupProperties.class})
@EnableScheduling
public class OutboxDispatcherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BackoffStrategy.class)
    public BackoffStrategy exponentialBackoffStrategy(OutboxDispatcherProperties properties) {
        return new ExponentialBackoffStrategy(properties.getBackoffBaseMs(), properties.getBackoffMaxMs());
    }

    @Bean
    @ConditionalOnBean({OutboxMessageStore.class, MessageSender.class, BackoffStrategy.class})
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    @ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "mode", havingValue = "scheduled", matchIfMissing = true)
    public ScheduledOutboxDispatcher scheduledOutboxDispatcher(
            OutboxMessageStore outboxRepository,
            MessageSender messageSender,
            BackoffStrategy backoffStrategy,
            OutboxDispatcherProperties properties) {
        return new ScheduledOutboxDispatcher(outboxRepository, messageSender,
                properties.getMaxRetries(), backoffStrategy, properties.getBatchSize());
    }

    /// P2-1 stuck-DISPATCHING recovery job.
    /// <p>
    /// Registered only when {@link OutboxMessageStore} exists. It is decoupled from dispatcher mode,
    /// so it can recover stuck records independently even when {@code mode=jobrunr}.
    @Bean
    @ConditionalOnBean({OutboxMessageStore.class})
    @ConditionalOnMissingBean(OutboxRecoveryJob.class)
    public OutboxRecoveryJob outboxRecoveryJob(OutboxMessageStore outboxRepository,
                                               OutboxRecoveryProperties recoveryProperties) {
        return new OutboxRecoveryJob(outboxRepository, recoveryProperties);
    }

    /// P2-5 terminal-state cleanup job.
    /// <p>
    /// Registered only when {@link OutboxMessageStore} exists. It is decoupled from dispatcher mode,
    /// so it can clean PUBLISHED / DEAD_LETTERED records independently even when {@code mode=jobrunr}.
    /// Enablement is controlled by {@link OutboxCleanupProperties#isEnabled()} (default
    /// {@code true}) without restarting the ApplicationContext.
    @Bean
    @ConditionalOnBean({OutboxMessageStore.class})
    @ConditionalOnMissingBean(OutboxCleanupJob.class)
    public OutboxCleanupJob outboxCleanupJob(OutboxMessageStore outboxRepository,
                                             OutboxCleanupProperties cleanupProperties) {
        return new OutboxCleanupJob(outboxRepository, cleanupProperties);
    }
}
