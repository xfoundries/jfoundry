package org.jfoundry.autoconfigure;

import org.jfoundry.autoconfigure.aop.JFoundryAopAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class AopInfrastructureAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JFoundryAopAutoConfiguration.class))
            .withUserConfiguration(LateBeanPostProcessorConfiguration.class);

    @Test
    void doesNotInitializeRuntimeDependenciesWhileBeanPostProcessorsAreBeingRegistered(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
            assertThat(context.getBeanNamesForType(AbstractAutoProxyCreator.class))
                    .containsExactly(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
            assertThat(output).doesNotContain("not eligible for getting processed by all BeanPostProcessors");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class LateBeanPostProcessorConfiguration {

        @Bean
        static BeanPostProcessor lateBeanPostProcessor() {
            return new BeanPostProcessor() {
            };
        }
    }
}
