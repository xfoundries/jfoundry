package org.jfoundry.autoconfigure.transaction;

import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.application.transaction.TransactionRunner;
import org.jfoundry.infrastructure.transaction.spring.ApplicationTransactionalInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

/// Auto-configures declarative application transaction interception after a transaction runner is
/// available, whether supplied by jfoundry or by the application.
@AutoConfiguration(after = TransactionRunnerAutoConfiguration.class)
@ConditionalOnClass({ApplicationTransactional.class, ApplicationTransactionalInterceptor.class, Advisor.class})
@ConditionalOnBean(TransactionRunner.class)
@ConditionalOnProperty(prefix = "jfoundry.application.transaction.annotation", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class ApplicationTransactionalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ApplicationTransactionalInterceptor applicationTransactionalInterceptor(
            TransactionRunner transactionRunner) {
        return new ApplicationTransactionalInterceptor(transactionRunner);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "applicationTransactionalAdvisor")
    static Advisor applicationTransactionalAdvisor() {
        ComposablePointcut pointcut = new ComposablePointcut(
                new AnnotationMatchingPointcut(ApplicationTransactional.class, true));
        pointcut.union(AnnotationMatchingPointcut.forMethodAnnotation(ApplicationTransactional.class));
        DefaultBeanFactoryPointcutAdvisor advisor = new DefaultBeanFactoryPointcutAdvisor();
        advisor.setPointcut(pointcut);
        advisor.setAdviceBeanName("applicationTransactionalInterceptor");
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return advisor;
    }
}
