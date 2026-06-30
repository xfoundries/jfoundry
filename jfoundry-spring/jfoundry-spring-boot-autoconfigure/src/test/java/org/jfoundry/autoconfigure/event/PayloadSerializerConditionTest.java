package org.jfoundry.autoconfigure.event;

import org.jfoundry.application.messaging.PayloadSerializer;
import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/// P2-4 regression: {@code payloadSerializer} 必须 {@code @ConditionalOnBean(ObjectMapper.class)}
/// —— 业务侧无 Jackson 时不应因找不到 ObjectMapper bean 而启动失败。
/// <p>
/// 同时验证传递性：{@code domainEventOutboxRecorder} 新增的
/// {@code @ConditionalOnBean(PayloadSerializer.class)} 守护也生效 —— 没有 serializer
/// 时 Outbox recorder 正确退让，而不是因为缺依赖报错。
class PayloadSerializerConditionTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(DomainEventOutboxRecorderAutoConfiguration.class))
                    .withBean(OutboxMessageStore.class, () -> mock(OutboxMessageStore.class));

    @Test
    void contextStartsWithoutFailureWhenObjectMapperBeanIsMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PayloadSerializer.class);
        });
    }

    @Test
    void outboxRecorderAlsoRetractsWhenPayloadSerializerMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DomainEventOutboxRecorder.class);
        });
    }
}
