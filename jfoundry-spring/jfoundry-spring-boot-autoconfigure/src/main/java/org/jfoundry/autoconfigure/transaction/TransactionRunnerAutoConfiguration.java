package org.jfoundry.autoconfigure.transaction;

import org.jfoundry.application.transaction.TransactionRunner;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.infrastructure.transaction.spring.ApplicationTransactionalInterceptor;
import org.jfoundry.infrastructure.transaction.spring.SpringTransactionRunner;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Auto-configuration for application-layer transaction boundaries.
 */
@AutoConfiguration
@ConditionalOnClass({
        TransactionRunner.class,
        TransactionTemplate.class
})
public class TransactionRunnerAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.jfoundry.infrastructure.transaction.spring.SpringTransactionRunner")
    static class SpringTransactionRunnerConfiguration {

        @Bean
        @ConditionalOnBean(PlatformTransactionManager.class)
        @ConditionalOnMissingBean(TransactionRunner.class)
        public SpringTransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
            return new SpringTransactionRunner(transactionManager);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ApplicationTransactional.class, ApplicationTransactionalInterceptor.class, Advisor.class})
    @ConditionalOnProperty(prefix = "jfoundry.application.transaction.annotation", name = "enabled",
                           havingValue = "true", matchIfMissing = true)
    static class ApplicationTransactionalAnnotationConfiguration {

        @Bean
        @ConditionalOnBean(TransactionRunner.class)
        @ConditionalOnMissingBean
        public ApplicationTransactionalInterceptor applicationTransactionalInterceptor(TransactionRunner transactionRunner) {
            return new ApplicationTransactionalInterceptor(transactionRunner);
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @ConditionalOnBean(ApplicationTransactionalInterceptor.class)
        @ConditionalOnMissingBean(name = "applicationTransactionalAdvisor")
        public Advisor applicationTransactionalAdvisor(ApplicationTransactionalInterceptor interceptor) {
            ComposablePointcut pointcut = new ComposablePointcut(
                    new AnnotationMatchingPointcut(ApplicationTransactional.class, true));
            pointcut.union(AnnotationMatchingPointcut.forMethodAnnotation(ApplicationTransactional.class));
            DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
            advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
            return advisor;
        }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @ConditionalOnBean(name = "applicationTransactionalAdvisor")
        @ConditionalOnMissingBean(AbstractAutoProxyCreator.class)
        public static InfrastructureAdvisorAutoProxyCreator applicationTransactionalAutoProxyCreator() {
            InfrastructureAdvisorAutoProxyCreator creator = new InfrastructureAdvisorAutoProxyCreator();
            creator.setProxyTargetClass(true);
            return creator;
        }
    }
}
