package org.jfoundry.autoconfigure.outbox.persistence;

import jakarta.persistence.EntityManager;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.outbox.jpa.JpaOutboxMessageStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// Auto-configures the Jakarta Persistence Outbox store.
@AutoConfiguration
@AutoConfigureBefore(name = "org.jfoundry.autoconfigure.outbox.dispatcher.OutboxDispatcherAutoConfiguration")
@ConditionalOnClass({EntityManager.class, JpaOutboxMessageStore.class})
@ConditionalOnBean(EntityManager.class)
public class OutboxJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OutboxMessageStore.class)
    public OutboxMessageStore jpaOutboxMessageStore(EntityManager entityManager) {
        return new JpaOutboxMessageStore(entityManager);
    }
}
