package org.jfoundry.autoconfigure;

import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.application.event.DomainEventDispatcher;
import org.jfoundry.autoconfigure.messaging.DomainEventDispatchInterceptor;
import org.jfoundry.infrastructure.messaging.spring.dispatcher.SpringDomainEventDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DomainEventDispatchAutoConfigurationTest.TestApp.class)
class DomainEventDispatchAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private DomainEventDispatcher domainEventDispatcher;

    @Autowired
    private DomainEventContext domainEventContext;

    @Autowired
    private DomainEventDispatchInterceptor interceptor;

    @Autowired
    private Advisor domainEventDispatchAdvisor;

    @Test
    void domainEventDispatchInfrastructureIsAutoConfigured() {
        assertThat(domainEventDispatcher).isInstanceOf(SpringDomainEventDispatcher.class);
        assertThat(domainEventContext).isNotNull();
        assertThat(interceptor).isNotNull();
        assertThat(domainEventDispatchAdvisor).isNotNull();
    }
}
