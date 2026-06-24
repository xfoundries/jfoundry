package org.jfoundry.autoconfigure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration;
import org.jfoundry.infrastructure.messaging.PayloadSerializer;
import org.jfoundry.infrastructure.messaging.externalization.AggregateRoutingResolver;
import org.jfoundry.infrastructure.messaging.externalization.ExternalizationRuleResolver;
import org.jfoundry.infrastructure.messaging.jackson.JacksonPayloadSerializer;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jfoundry.infrastructure.outbox.spring.externalization.DomainEventExternalizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/// DomainEventExternalizer 自动配置。
/// <p>
/// 当业务侧提供 OutboxRepository Bean 时启用 DomainEventExternalizer Sink。
/// 默认 PayloadSerializer 使用 Jackson 实现；业务侧注册自己的 {@link PayloadSerializer} Bean 即可覆盖。
/// <p>
/// {@code payloadSerializer} 只在 classpath 上有 Jackson 时注册：避免业务侧不引入
/// Jackson 时 autoconfig 因为找不到 ObjectMapper bean 而启动失败。
/// <p>
/// {@code domainEventExternalizer} 同时要求 OutboxRepository + PayloadSerializer 都存在 ——
/// PayloadSerializer 缺失时（业务侧无 Jackson 也未自定义 serializer），Externalizer 缺少
/// 必需依赖，正确退让而不是让上下文启动失败。业务侧无 Jackson 时需要自己注册
/// {@link PayloadSerializer} 才能恢复 Externalizer 行为。
/// <p>
/// {@code domainEventExternalizer} 仅在业务侧未提供自己的 {@link DomainEventExternalizer} 时注册。
/// 它是 Sink 链的末端（{@link Ordered#LOWEST_PRECEDENCE}），业务侧自定义的 {@code DomainEventSink}
/// （日志、metrics 等）可以与之共存。
@AutoConfiguration
@AutoConfigureAfter(OutboxMybatisPlusAutoConfiguration.class)
public class DomainEventExternalizerAutoConfiguration {

    /// 必须在 {@link #domainEventExternalizer} 之前声明：{@code @ConditionalOnBean} 在
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
    @ConditionalOnBean({OutboxRepository.class, PayloadSerializer.class})
    @ConditionalOnMissingBean(DomainEventExternalizer.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public DomainEventExternalizer domainEventExternalizer(
            OutboxRepository outboxRepository,
            ExternalizationRuleResolver ruleResolver,
            AggregateRoutingResolver aggregateRoutingResolver,
            PayloadSerializer serializer) {
        return new DomainEventExternalizer(outboxRepository, serializer, ruleResolver, aggregateRoutingResolver);
    }
}
