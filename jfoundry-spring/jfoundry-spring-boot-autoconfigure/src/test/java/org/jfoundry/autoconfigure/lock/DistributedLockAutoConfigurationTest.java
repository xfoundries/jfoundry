package org.jfoundry.autoconfigure.lock;

import org.jfoundry.application.lock.DistributedLockClient;
import org.jfoundry.application.lock.LockHandle;
import org.jfoundry.application.lock.LockTemplate;
import org.jfoundry.infrastructure.lock.redisson.RedissonDistributedLockClient;
import org.jfoundry.infrastructure.lock.spring.DistributedLockInterceptor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.aop.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DistributedLockAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class));

    @Test
    void registersTemplateAndAdvisorWhenLockClientExists() {
        runner.withBean(DistributedLockClient.class, () -> (name, options) -> new LockHandle(name, true))
                .run(context -> {
                    assertThat(context).hasSingleBean(LockTemplate.class);
                    assertThat(context).hasSingleBean(DistributedLockInterceptor.class);
                    assertThat(context).hasBean("distributedLockAdvisor");
                    assertThat(context.getBean("distributedLockAdvisor")).isInstanceOf(Advisor.class);
                });
    }

    @Test
    void createsRedissonLockClientWhenRedissonClientExists() {
        runner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedLockClient.class);
                    assertThat(context.getBean(DistributedLockClient.class)).isInstanceOf(RedissonDistributedLockClient.class);
                    assertThat(context).hasSingleBean(LockTemplate.class);
                });
    }

    @Test
    void canDisableAnnotationAdvisor() {
        runner.withBean(DistributedLockClient.class, () -> (name, options) -> new LockHandle(name, true))
                .withPropertyValues("jfoundry.lock.annotation.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(LockTemplate.class);
                    assertThat(context).doesNotHaveBean(DistributedLockInterceptor.class);
                    assertThat(context).doesNotHaveBean("distributedLockAdvisor");
                });
    }
}
