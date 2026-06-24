package org.jfoundry.autoconfigure.outbox.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.SendResult;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.OutboxDispatcher;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jfoundry.infrastructure.outbox.jobrunr.dispatcher.JobRunrOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// P2-1 regression: {@link JobRunrDispatcherAutoConfiguration} 必须由
/// jfoundry-autoconfigure 的 {@code META-INF/spring/...AutoConfiguration.imports} 注册，并在
/// {@code mode=jobrunr} 时把 {@link JobRunrOutboxDispatcher} 注册为
/// {@link OutboxDispatcher} bean。
/// <p>
/// 用 {@link ApplicationContextRunner} + {@link AutoConfigurations#of} 而不是
/// {@code @SpringBootTest}：避免触发 JobRunr 自带的 autoconfig
/// ({@code BackgroundJobServer} / dashboard)，那些需要完整 DataSource 和
/// schema 才能启动，与本测试无关。
class JobRunrDispatcherAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(JobRunrDispatcherAutoConfiguration.class))
                    .withBean(OutboxRepository.class, () -> mock(OutboxRepository.class))
                    .withBean(MessageSender.class, () -> (MessageSender) (topic, key, payload) -> SendResult.ok())
                    .withBean(BackoffStrategy.class, () -> (BackoffStrategy) failedAttempts -> Duration.ofSeconds(1));

    @Test
    void jobRunrDispatcherBeanIsRegisteredWhenModeIsJobRunr() {
        runner
                .withPropertyValues(
                        "jfoundry.outbox.dispatcher.mode=jobrunr",
                        "jfoundry.outbox.dispatcher.batchSize=20",
                        "jfoundry.outbox.dispatcher.maxRetries=7"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDispatcher.class);
                    assertThat(context.getBean(OutboxDispatcher.class))
                            .isInstanceOf(JobRunrOutboxDispatcher.class);
                    assertThat(context).hasSingleBean(JobRunrOutboxDispatcher.class);
                });
    }

    @Test
    void dispatcherBeanIsAbsentWhenModeIsScheduled() {
        runner
                .withPropertyValues("jfoundry.outbox.dispatcher.mode=scheduled")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxDispatcher.class);
                    assertThat(context).doesNotHaveBean(JobRunrOutboxDispatcher.class);
                });
    }

    @Test
    void dispatcherBeanIsAbsentWhenModeIsMissing() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(OutboxDispatcher.class);
            assertThat(context).doesNotHaveBean(JobRunrOutboxDispatcher.class);
        });
    }

    @Test
    void batchSizeIsInjectedFromProperties() {
        runner
                .withPropertyValues(
                        "jfoundry.outbox.dispatcher.mode=jobrunr",
                        "jfoundry.outbox.dispatcher.batchSize=20",
                        "jfoundry.outbox.dispatcher.maxRetries=7"
                )
                .run(context -> {
                    OutboxRepository repo = context.getBean(OutboxRepository.class);
                    when(repo.claimDispatchable(anyInt(), any())).thenReturn(List.of());

                    // recurringDispatch 走构造函数注入的 batchSize 字段（@Job 入口），
                    // 而不是 dispatch(int) 的入参 —— 这是 properties 注入实际起作用的路径。
                    context.getBean(JobRunrOutboxDispatcher.class).recurringDispatch();

                    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
                    verify(repo).claimDispatchable(captor.capture(), any());
                    assertThat(captor.getValue())
                            .as("batchSize 必须来自 jfoundry.outbox.dispatcher.batchSize=20")
                            .isEqualTo(20);
                });
    }

    @Test
    void dispatcherIsAbsentWhenOutboxRepositoryBeanMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JobRunrDispatcherAutoConfiguration.class))
                .withBean(MessageSender.class, () -> (MessageSender) (topic, key, payload) -> SendResult.ok())
                .withBean(BackoffStrategy.class, () -> (BackoffStrategy) failedAttempts -> Duration.ofSeconds(1))
                .withPropertyValues("jfoundry.outbox.dispatcher.mode=jobrunr")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxDispatcher.class);
                    assertThat(context).hasNotFailed();
                });
    }
}
