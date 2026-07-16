package org.jfoundry.autoconfigure.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.jfoundry.application.inbox.InboxMessageStore;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxClaimStrategy;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxMessageEntity;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxMessageStore;
import org.jfoundry.infrastructure.outbox.jpa.JpaOutboxMessageEntity;
import org.jfoundry.infrastructure.outbox.jpa.JpaOutboxMessageStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JpaStoreAutoConfigurationBootTest.Application.class, properties = {
        "jfoundry.outbox.dispatcher.mode=none",
        "spring.datasource.url=jdbc:h2:mem:jpa-store-auto-configuration;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.jfoundry.autoconfigure.inbox.InboxMybatisPlusAutoConfiguration,org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration"
})
class JpaStoreAutoConfigurationBootTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private OutboxMessageStore outboxMessageStore;

    @Autowired
    private InboxMessageStore inboxMessageStore;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void bootsWithTheStandardJpaFactoryAndMapsApplicationAndFrameworkEntities() {
        assertThat(outboxMessageStore).isInstanceOf(JpaOutboxMessageStore.class);
        assertThat(inboxMessageStore).isInstanceOf(JpaInboxMessageStore.class);
        assertThat(entityManagerFactory.getMetamodel().entity(JpaStoreApplicationEntity.class)).isNotNull();
        assertThat(entityManagerFactory.getMetamodel().entity(JpaOutboxMessageEntity.class)).isNotNull();
        assertThat(entityManagerFactory.getMetamodel().entity(JpaInboxMessageEntity.class)).isNotNull();

        transactions.executeWithoutResult(ignored -> outboxMessageStore.append(
                OutboxMessage.newPending("evt-1", "topic", null, "example.Event", "{}", Instant.now())));

        List<OutboxMessage> dispatchable = transactions.execute(
                ignored -> outboxMessageStore.findDispatchable(1, Instant.now()));
        assertThat(dispatchable)
                .extracting(OutboxMessage::getEventId)
                .containsExactly("evt-1");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {

        @Bean
        JpaInboxClaimStrategy h2JpaInboxClaimStrategy() {
            return (entityManager, messageId, consumerName, now) -> false;
        }
    }
}
