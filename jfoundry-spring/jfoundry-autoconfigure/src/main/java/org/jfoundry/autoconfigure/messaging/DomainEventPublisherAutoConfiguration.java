package org.jfoundry.autoconfigure.messaging;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.infrastructure.messaging.externalization.DomainEventSink;
import org.jfoundry.infrastructure.messaging.spring.publisher.SpringDomainEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.List;

/// 自动注册 {@link SpringDomainEventPublisher} 作为默认 {@link DomainEventPublisher}。
/// <p>
/// 业务侧如需自定义发布器，注册自己的 {@code DomainEventPublisher} Bean 即可覆盖。
/// <p>
/// 开关：{@code jfoundry.domain.event.enabled=false} 可关闭默认注册。
@AutoConfiguration
@ConditionalOnClass({DomainEventPublisher.class, SpringDomainEventPublisher.class})
@ConditionalOnProperty(prefix = "jfoundry.domain.event", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class DomainEventPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public SpringDomainEventPublisher springDomainEventPublisher(
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<List<DomainEventSink>> sinksProvider) {
        List<DomainEventSink> sinks = sinksProvider.getIfAvailable(List::of);
        return new SpringDomainEventPublisher(eventPublisher, sinks);
    }
}
