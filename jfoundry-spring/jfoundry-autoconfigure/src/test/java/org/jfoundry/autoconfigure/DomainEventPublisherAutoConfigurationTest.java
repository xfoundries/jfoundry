package org.jfoundry.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.domain.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/// P1-1 regression test: SpringDomainEventPublisher must be registered via autoconfig
/// so that a minimal Spring Boot app with jfoundry-spring-autoconfigure on classpath
/// can inject DomainEventPublisher without business-side @ComponentScan.
/// <p>
/// TestApp provides an ObjectMapper bean because DomainEventExternalizerAutoConfiguration's
/// unconditional payloadSerializer bean method requires Jackson. Marking payloadSerializer
/// conditional on ObjectMapper is out of scope for Task 1.2 (which only corrects the
/// domainEventExternalizer bean's condition type).
@SpringBootTest(classes = DomainEventPublisherAutoConfigurationTest.TestApp.class)
class DomainEventPublisherAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private DomainEventPublisher publisher;

    @Test
    void domainEventPublisherIsAutoConfigured() {
        assertThat(publisher).isNotNull();
    }
}
