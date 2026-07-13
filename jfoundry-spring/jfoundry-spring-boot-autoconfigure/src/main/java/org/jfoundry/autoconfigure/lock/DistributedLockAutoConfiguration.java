package org.jfoundry.autoconfigure.lock;

import org.jfoundry.application.lock.DistributedLock;
import org.jfoundry.application.lock.DistributedLockClient;
import org.jfoundry.application.lock.LockTemplate;
import org.jfoundry.infrastructure.lock.redisson.RedissonDistributedLockClient;
import org.jfoundry.infrastructure.lock.spring.DistributedLockInterceptor;
import org.redisson.api.RedissonClient;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for distributed lock support.
 */
@AutoConfiguration
@AutoConfigureAfter(name = "org.redisson.spring.starter.RedissonAutoConfigurationV2")
@ConditionalOnClass({DistributedLockClient.class, LockTemplate.class})
public class DistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnBean(DistributedLockClient.class)
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(DistributedLockClient lockClient) {
        return new LockTemplate(lockClient);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedissonClient.class, RedissonDistributedLockClient.class})
    static class RedissonLockConfiguration {

        @Bean
        @ConditionalOnBean(RedissonClient.class)
        @ConditionalOnMissingBean(DistributedLockClient.class)
        public RedissonDistributedLockClient redissonDistributedLockClient(RedissonClient redissonClient) {
            return new RedissonDistributedLockClient(redissonClient);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({DistributedLock.class, DistributedLockInterceptor.class, Advisor.class})
    @ConditionalOnBean(DistributedLockClient.class)
    @ConditionalOnProperty(prefix = "jfoundry.lock.annotation", name = "enabled",
                           havingValue = "true", matchIfMissing = true)
    static class DistributedLockAnnotationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public DistributedLockInterceptor distributedLockInterceptor(LockTemplate lockTemplate) {
            return new DistributedLockInterceptor(lockTemplate);
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @ConditionalOnBean(DistributedLockInterceptor.class)
        @ConditionalOnMissingBean(name = "distributedLockAdvisor")
        public Advisor distributedLockAdvisor(DistributedLockInterceptor interceptor) {
            DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
                    AnnotationMatchingPointcut.forMethodAnnotation(DistributedLock.class), interceptor);
            advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 200);
            return advisor;
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @ConditionalOnBean(name = "distributedLockAdvisor")
        @ConditionalOnMissingBean(AbstractAutoProxyCreator.class)
        public static InfrastructureAdvisorAutoProxyCreator distributedLockAutoProxyCreator() {
            InfrastructureAdvisorAutoProxyCreator creator = new InfrastructureAdvisorAutoProxyCreator();
            creator.setProxyTargetClass(true);
            return creator;
        }
    }
}
