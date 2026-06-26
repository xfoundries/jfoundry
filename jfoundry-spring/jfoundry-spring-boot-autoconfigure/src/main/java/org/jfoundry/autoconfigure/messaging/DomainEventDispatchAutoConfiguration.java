package org.jfoundry.autoconfigure.messaging;

import org.jfoundry.application.ApplicationService;
import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.application.event.DomainEventDispatcher;
import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jfoundry.infrastructure.messaging.spring.dispatcher.SpringDomainEventDispatcher;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

@AutoConfiguration
@EnableConfigurationProperties(DomainEventDispatchProperties.class)
@ConditionalOnClass({
        ApplicationService.class,
        DomainEventDispatcher.class,
        SpringDomainEventDispatcher.class,
        Advisor.class
})
@ConditionalOnProperty(prefix = "jfoundry.domain.event", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class DomainEventDispatchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DomainEventScope domainEventScope() {
        return new DomainEventScope();
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventContext.class)
    public ScopedValueDomainEventContext domainEventContext(DomainEventScope scope) {
        return new ScopedValueDomainEventContext(scope);
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventDispatcher.class)
    public SpringDomainEventDispatcher springDomainEventDispatcher(
            ObjectProvider<DomainEventOutboxRecorder> outboxRecorderProvider,
            ApplicationEventPublisher eventPublisher) {
        return new SpringDomainEventDispatcher(outboxRecorderProvider.getIfAvailable(), eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainEventDispatchInterceptor domainEventDispatchInterceptor(
            DomainEventScope scope,
            DomainEventDispatcher dispatcher) {
        return new DomainEventDispatchInterceptor(scope, dispatcher);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "domainEventDispatchAdvisor")
    public Advisor domainEventDispatchAdvisor(DomainEventDispatchInterceptor interceptor) {
        return new DefaultPointcutAdvisor(new AnnotationMatchingPointcut(ApplicationService.class, true), interceptor);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(AbstractAutoProxyCreator.class)
    public static InfrastructureAdvisorAutoProxyCreator domainEventDispatchAutoProxyCreator() {
        InfrastructureAdvisorAutoProxyCreator creator = new InfrastructureAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }
}
