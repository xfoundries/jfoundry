package org.jfoundry.autoconfigure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.application.messaging.PayloadSerializer;
import org.jfoundry.application.messaging.externalization.AggregateRoutingResolver;
import org.jfoundry.application.messaging.externalization.ExternalizationRuleResolver;
import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.messaging.jackson.JacksonPayloadSerializer;
import org.jfoundry.infrastructure.outbox.spring.externalization.DefaultDomainEventOutboxRecorder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// DomainEventOutboxRecorder 自动配置。
/// <p>
/// 当业务侧提供 OutboxMessageStore Bean 时启用默认 DomainEventOutboxRecorder。
/// 默认 PayloadSerializer 使用 Jackson 实现；业务侧注册自己的 {@link PayloadSerializer} Bean 即可覆盖。
/// <p>
/// {@code payloadSerializer} 只在 classpath 上有 Jackson 时注册：避免业务侧不引入
/// Jackson 时 autoconfig 因为找不到 ObjectMapper bean 而启动失败。
/// <p>
/// {@code domainEventOutboxRecorder} 同时要求 OutboxMessageStore + PayloadSerializer 都存在 ——
/// PayloadSerializer 缺失时（业务侧无 Jackson 也未自定义 serializer），Outbox recorder 缺少
/// 必需依赖，正确退让而不是让上下文启动失败。业务侧无 Jackson 时需要自己注册
/// {@link PayloadSerializer} 才能恢复 Outbox 写入行为。
/// <p>
/// {@code domainEventOutboxRecorder} 仅在业务侧未提供自己的 {@link DomainEventOutboxRecorder} 时注册。
@AutoConfiguration
@AutoConfigureAfter(name = "org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration")
@ConditionalOnClass({PayloadSerializer.class, JacksonPayloadSerializer.class, OutboxMessageStore.class, DefaultDomainEventOutboxRecorder.class})
public class DomainEventOutboxRecorderAutoConfiguration {

    /// 必须在 {@link #domainEventOutboxRecorder} 之前声明：{@code @ConditionalOnBean} 在
    /// bean 定义注册阶段按声明顺序评估，下游 bean 必须晚于其依赖的 bean 声明，
    /// 否则在评估时上游 bean 尚未注册，条件会误判为 false。
    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnMissingBean(PayloadSerializer.class)
    public PayloadSerializer payloadSerializer(ObjectMapper objectMapper) {
        return new JacksonPayloadSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalizationRuleResolver.class)
    public ExternalizationRuleResolver externalizationRuleResolver() {
        return new ExternalizationRuleResolver();
    }

    @Bean
    @ConditionalOnMissingBean(AggregateRoutingResolver.class)
    public AggregateRoutingResolver aggregateRoutingResolver() {
        return new AggregateRoutingResolver();
    }

    @Bean
    @ConditionalOnBean({OutboxMessageStore.class, PayloadSerializer.class})
    @ConditionalOnMissingBean(DomainEventOutboxRecorder.class)
    public DefaultDomainEventOutboxRecorder domainEventOutboxRecorder(
            OutboxMessageStore outboxRepository,
            ExternalizationRuleResolver ruleResolver,
            AggregateRoutingResolver aggregateRoutingResolver,
            PayloadSerializer serializer) {
        return new DefaultDomainEventOutboxRecorder(outboxRepository, serializer, ruleResolver, aggregateRoutingResolver);
    }
}
