package org.jfoundry.autoconfigure.outbox.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.OutboxDispatcher;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jfoundry.infrastructure.outbox.jobrunr.dispatcher.JobRunrOutboxDispatcher;
import org.jfoundry.infrastructure.outbox.spring.dispatcher.OutboxDispatcherProperties;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// JobRunr Outbox Dispatcher 自动装配。
/// <p>
/// 引入 {@code jfoundry-outbox-jobrunr} jar 并设置
/// {@code jfoundry.outbox.dispatcher.mode=jobrunr} 时，本类自动注册
/// {@link JobRunrOutboxDispatcher} bean，覆盖 {@code scheduled} 模式下的
/// {@code ScheduledOutboxDispatcher}（后者由 {@code OutboxDispatcherAutoConfiguration}
/// 在 mode=scheduled 或 matchIfMissing 时注册，互斥键是 bean name）。
/// <p>
/// 业务侧无需自己 {@code @ComponentScan} 扫到 {@code org.jfoundry.infrastructure.outbox.jobrunr}
/// —— Spring Boot starter 通过 jfoundry-autoconfigure 的
/// {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册本配置。
/// <p>
/// batchSize / maxRetries / cron 全部从 {@link OutboxDispatcherProperties} 读取，与 scheduled
/// 模式行为一致（同一套 {@code jfoundry.outbox.dispatcher.*} 配置）。
@AutoConfiguration
@ConditionalOnClass(name = "org.jobrunr.jobs.annotations.Job")
@ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "mode", havingValue = "jobrunr")
@EnableConfigurationProperties(OutboxDispatcherProperties.class)
public class JobRunrDispatcherAutoConfiguration {

    @Bean
    @ConditionalOnBean({OutboxRepository.class, MessageSender.class, BackoffStrategy.class})
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    public JobRunrOutboxDispatcher jobRunrOutboxDispatcher(
            OutboxRepository outboxRepository,
            MessageSender messageSender,
            BackoffStrategy backoffStrategy,
            OutboxDispatcherProperties properties,
            ObjectProvider<JobScheduler> jobScheduler) {
        JobRunrOutboxDispatcher dispatcher = new JobRunrOutboxDispatcher(
                outboxRepository,
                messageSender,
                properties.getBatchSize(),
                properties.getMaxRetries(),
                backoffStrategy);
        jobScheduler.ifAvailable(scheduler -> scheduler.scheduleRecurrently(
                "jfoundry-outbox-dispatch",
                properties.getCron(),
                dispatcher::recurringDispatch));
        return dispatcher;
    }
}
