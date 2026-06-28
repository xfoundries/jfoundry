package org.jfoundry.autoconfigure;

import org.jfoundry.application.event.CompositeDomainEventDispatcher;
import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.application.event.DomainEventDispatcher;
import org.jfoundry.application.outbox.DomainEventOutboxRecorder;
import org.jfoundry.autoconfigure.messaging.DomainEventDispatchAutoConfiguration;
import org.jfoundry.autoconfigure.messaging.DomainEventDispatchInterceptor;
import org.jfoundry.autoconfigure.messaging.DomainEventScope;
import org.jfoundry.infrastructure.messaging.spring.dispatcher.SpringApplicationEventDispatcher;
import org.jfoundry.infrastructure.outbox.spring.externalization.OutboxDomainEventDispatcher;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventDispatchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DomainEventDispatchAutoConfiguration.class))
            .withBean(ApplicationEventPublisher.class, () -> event -> {
            });

    @Test
    void defaultConfigurationRegistersContextAndSpringDispatcher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DomainEventScope.class);
            assertThat(context).hasSingleBean(DomainEventContext.class);
            assertThat(context).hasSingleBean(SpringApplicationEventDispatcher.class);
            assertThat(context).hasSingleBean(CompositeDomainEventDispatcher.class);
            assertThat(context.getBean(DomainEventDispatcher.class)).isInstanceOf(CompositeDomainEventDispatcher.class);
            assertThat(context).hasSingleBean(DomainEventDispatchInterceptor.class);
            assertThat(context).hasBean("domainEventDispatchAdvisor");
            assertThat(context.getBean("domainEventDispatchAdvisor")).isInstanceOf(Advisor.class);
        });
    }

    @Test
    void disabledDispatchKeepsContextButDoesNotRegisterDispatchInfrastructure() {
        contextRunner
                .withPropertyValues("jfoundry.domain.event.dispatch.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventScope.class);
                    assertThat(context).hasSingleBean(DomainEventContext.class);
                    assertThat(context).doesNotHaveBean(DomainEventDispatcher.class);
                    assertThat(context).doesNotHaveBean(DomainEventDispatchInterceptor.class);
                    assertThat(context).doesNotHaveBean("domainEventDispatchAdvisor");
                });
    }

    @Test
    void legacyDomainEventEnabledPropertyDoesNotDisableDispatchInfrastructure() {
        contextRunner
                .withPropertyValues("jfoundry.domain.event.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventScope.class);
                    assertThat(context).hasSingleBean(DomainEventContext.class);
                    assertThat(context).hasSingleBean(SpringApplicationEventDispatcher.class);
                    assertThat(context.getBean(DomainEventDispatcher.class)).isInstanceOf(CompositeDomainEventDispatcher.class);
                    assertThat(context).hasSingleBean(DomainEventDispatchInterceptor.class);
                    assertThat(context).hasBean("domainEventDispatchAdvisor");
                });
    }

    @Test
    void missingSpringMessagingModuleDoesNotBreakMinimalStarter() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.jfoundry.infrastructure.messaging.spring"))
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventScope.class);
                    assertThat(context).hasSingleBean(DomainEventContext.class);
                    assertThat(context).doesNotHaveBean(DomainEventDispatcher.class);
                    assertThat(context).doesNotHaveBean(DomainEventDispatchInterceptor.class);
                    assertThat(context).doesNotHaveBean("domainEventDispatchAdvisor");
                });
    }

    @Test
    void disabledSpringDispatcherDoesNotRegisterSpringPublisher() {
        contextRunner
                .withPropertyValues("jfoundry.domain.event.dispatch.spring.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DomainEventContext.class);
                    assertThat(context).doesNotHaveBean(SpringApplicationEventDispatcher.class);
                    assertThat(context).doesNotHaveBean(DomainEventDispatcher.class);
                    assertThat(context).doesNotHaveBean(DomainEventDispatchInterceptor.class);
                });
    }

    @Test
    void enabledOutboxDispatcherParticipatesInCompositeDispatcher() {
        contextRunner
                .withUserConfiguration(OutboxRecorderConfiguration.class)
                .withPropertyValues("jfoundry.domain.event.dispatch.outbox.enabled=true")
                .run(context -> {
                    TestOutboxRecorder recorder = context.getBean(TestOutboxRecorder.class);
                    DomainEventDispatcher dispatcher = context.getBean(DomainEventDispatcher.class);

                    dispatcher.dispatch(List.of(new TestEvent("order-1")));

                    assertThat(context).hasSingleBean(SpringApplicationEventDispatcher.class);
                    assertThat(context).hasSingleBean(OutboxDomainEventDispatcher.class);
                    assertThat(dispatcher).isInstanceOf(CompositeDomainEventDispatcher.class);
                    assertThat(recorder.recordedEvents).extracting(TestEvent::id).containsExactly("order-1");
                });
    }

    @Configuration
    static class OutboxRecorderConfiguration {

        @Bean
        TestOutboxRecorder testOutboxRecorder() {
            return new TestOutboxRecorder();
        }
    }

    static final class TestOutboxRecorder implements DomainEventOutboxRecorder {

        private final List<TestEvent> recordedEvents = new ArrayList<>();

        @Override
        public void record(List<? extends DomainEvent> events) {
            for (DomainEvent event : events) {
                recordedEvents.add((TestEvent) event);
            }
        }
    }

    record TestEvent(String id) implements DomainEvent {
    }
}
