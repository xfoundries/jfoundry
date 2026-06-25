package org.jfoundry.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.domain.event.AbstractDomainEvent;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.jfoundry.infrastructure.messaging.spring.publisher.SpringDomainEventPublisher;
import org.awaitility.Awaitility;
import org.jmolecules.event.annotation.Externalized;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/// 端到端集成测试：领域事件 → SpringDomainEventPublisher → DomainEventExternalizer Sink →
/// Outbox 表 → ScheduledOutboxDispatcher → CollectingMessageSender。
/// <p>
/// 通过 @EnableAutoConfiguration 让 Spring Boot 按 @AutoConfigureAfter 排序加载所有
/// jfoundry-spring-boot-autoconfigure 内部的 AutoConfiguration 链路；测试启动类自身负责
/// @MapperScan 以便 mapper bean 在 ConfigurationClassParser 阶段注册。
@SpringBootTest(classes = {
        DomainEventExternalizationIntegrationTest.TestApp.class,
        DomainEventExternalizationIntegrationTest.TestConfig.class
})
class DomainEventExternalizationIntegrationTest {

    @Autowired
    private SpringDomainEventPublisher publisher;

    @Autowired
    private CollectingMessageSender collectingSender;

    @Externalized("env.created")
    static class EnvCreatedEvent extends AbstractDomainEvent {
    }

    @Test
    void publishEventEndsUpAtMessageSender() {
        publisher.publish(new EnvCreatedEvent());

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(collectingSender.receivedPayloads).hasSize(1));
        assertThat(collectingSender.receivedPayloads.get(0)).contains("EnvCreatedEvent");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.outbox.mybatis")
    static class TestApp {
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        @Primary
        CollectingMessageSender collectingMessageSender() {
            return new CollectingMessageSender();
        }
    }

    static class CollectingMessageSender implements MessageSender {
        final List<String> receivedPayloads = new ArrayList<>();

        @Override
        public SendResult send(String topic, String payloadKey, String payload) {
            receivedPayloads.add(payload);
            return SendResult.ok();
        }
    }
}
