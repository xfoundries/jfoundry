package org.jfoundry.autoconfigure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.autoconfigure.persistence.OutboxMybatisPlusAutoConfiguration;
import org.jfoundry.infrastructure.messaging.JacksonPayloadSerializer;
import org.jfoundry.infrastructure.messaging.PayloadSerializer;
import org.jfoundry.infrastructure.messaging.externalization.ExternalizationRuleResolver;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.spring.externalization.DomainEventExternalizer;
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
/// {@code domainEventExternalizer} 仅在业务侧未提供自己的 {@link DomainEventExternalizer} 时注册。
/// 它是 Sink 链的末端（{@link Ordered#LOWEST_PRECEDENCE}），业务侧自定义的 {@code DomainEventSink}
/// （日志、metrics 等）可以与之共存。
@AutoConfiguration
@AutoConfigureAfter(OutboxMybatisPlusAutoConfiguration.class)
public class DomainEventExternalizerAutoConfiguration {

    @Bean
    @ConditionalOnBean(OutboxRepository.class)
    @ConditionalOnMissingBean(DomainEventExternalizer.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public DomainEventExternalizer domainEventExternalizer(
            OutboxRepository outboxRepository,
            ExternalizationRuleResolver ruleResolver,
            PayloadSerializer serializer) {
        return new DomainEventExternalizer(outboxRepository, serializer, ruleResolver);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalizationRuleResolver.class)
    public ExternalizationRuleResolver externalizationRuleResolver() {
        return new ExternalizationRuleResolver();
    }

    @Bean
    @ConditionalOnMissingBean(PayloadSerializer.class)
    public PayloadSerializer payloadSerializer(ObjectMapper objectMapper) {
        return new JacksonPayloadSerializer(objectMapper);
    }
}
