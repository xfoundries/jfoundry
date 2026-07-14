package org.jfoundry.autoconfigure.aop;

import org.springframework.aop.Advisor;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/// Coordinates the single Spring auto-proxy creator used by jfoundry advisors.
@AutoConfiguration(beforeName = {
        "org.jfoundry.autoconfigure.event.DomainEventDispatchAutoConfiguration",
        "org.jfoundry.autoconfigure.transaction.ApplicationTransactionalAutoConfiguration",
        "org.jfoundry.autoconfigure.lock.DistributedLockAutoConfiguration"
})
@ConditionalOnClass(Advisor.class)
@Import(JFoundryAopAutoConfiguration.AutoProxyRegistrar.class)
public class JFoundryAopAutoConfiguration {

    static final class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                            BeanDefinitionRegistry registry) {
            BeanDefinition registered = AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
            if (registered != null) {
                registered.getPropertyValues().add("proxyTargetClass", true);
            }
        }
    }
}
