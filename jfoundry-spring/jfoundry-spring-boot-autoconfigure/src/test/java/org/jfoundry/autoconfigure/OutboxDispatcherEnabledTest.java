package org.jfoundry.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.application.outbox.OutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/// P1-4 regression: jfoundry.outbox.dispatcher.enabled=false must actually disable
/// dispatcher bean registration. The existing config javadoc admitted the bug
/// ("业务侧需要禁用调度时...自行关闭 @EnableScheduling") — this test pins the fix.
/// <p>
/// TestApp provides an ObjectMapper bean so DomainEventOutboxRecorderAutoConfiguration's
/// payloadSerializer（@ConditionalOnBean(ObjectMapper.class)）能正常注册；
/// 同时让 DomainEventOutboxRecorder 的依赖链完整。
@SpringBootTest(
        classes = {OutboxDispatcherEnabledTest.TestApp.class, OutboxDispatcherEnabledTest.DisabledConfig.class},
        properties = "jfoundry.outbox.dispatcher.enabled=false"
)
class OutboxDispatcherEnabledTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @TestConfiguration
    static class DisabledConfig {
        // no bean overrides; property alone disables the autoconfig
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void noOutboxDispatcherBeanWhenDisabled() {
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(OutboxDispatcher.class));
    }

    @Test
    void noScheduledOutboxDispatcherBeanWhenDisabled() {
        assertThat(context.containsBeanDefinition("scheduledOutboxDispatcher"))
                .as("scheduledOutboxDispatcher bean must not be registered when enabled=false")
                .isFalse();
    }
}
