package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.jfoundry.application.inbox.InboxMessage;
import org.jfoundry.application.inbox.InboxMessageStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class JpaInboxMessageStoreTest {

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private AtomicInteger claimAttempts;
    private JpaInboxMessageStore store;

    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("jfoundry-inbox-jpa-test");
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        claimAttempts = new AtomicInteger();
        store = new JpaInboxMessageStore(entityManager, (manager, messageId, consumerName, now) -> {
            claimAttempts.incrementAndGet();
            manager.persist(JpaInboxMessageEntity.fromMessage(InboxMessage.processing(messageId, consumerName)));
            return true;
        });
        inTransaction(() -> entityManager.createQuery("delete from JpaInboxMessageEntity").executeUpdate());
    }

    @AfterEach
    void closeEntityManager() {
        entityManager.close();
    }

    @Test
    void retriesFailedMessagesWithACasTransitionToProcessing() {
        persist(failed("msg-1", "billing", "temporary failure"));

        boolean started = inTransactionResult(() -> store.tryStartProcessing("msg-1", "billing"));

        assertThat(started).isTrue();
        assertThat(claimAttempts).hasValue(0);
        InboxMessage message = load("msg-1", "billing");
        assertThat(message.getStatus()).isEqualTo(InboxMessageStatus.PROCESSING);
        assertThat(message.getErrorMessage()).isNull();
    }

    @Test
    void doesNotReclaimProcessedMessagesOrInvokeTheAbsentRowStrategy() {
        persist(InboxMessage.processed("msg-1", "billing"));

        boolean started = inTransactionResult(() -> store.tryStartProcessing("msg-1", "billing"));

        assertThat(started).isFalse();
        assertThat(claimAttempts).hasValue(0);
        assertThat(store.isProcessed("msg-1", "billing")).isTrue();
    }

    @Test
    void invokesTheInjectedClaimStrategyForAnAbsentMessage() {
        boolean started = inTransactionResult(() -> store.tryStartProcessing("msg-1", "billing"));

        assertThat(started).isTrue();
        assertThat(claimAttempts).hasValue(1);
        assertThat(load("msg-1", "billing").getStatus()).isEqualTo(InboxMessageStatus.PROCESSING);
    }

    @Test
    void allowsDifferentConsumersToProcessTheSameMessageIndependently() {
        assertThat(inTransactionResult(() -> store.tryStartProcessing("msg-1", "billing"))).isTrue();
        assertThat(inTransactionResult(() -> store.tryStartProcessing("msg-1", "analytics"))).isTrue();
        inTransaction(() -> store.markProcessed("msg-1", "billing"));
        inTransaction(() -> store.markProcessed("msg-1", "analytics"));

        assertThat(store.isProcessed("msg-1", "billing")).isTrue();
        assertThat(store.isProcessed("msg-1", "analytics")).isTrue();
        assertThat(count("msg-1")).isEqualTo(2);
    }

    @Test
    void staleCompletionCannotOverwriteAFailedMessage() {
        assertThat(inTransactionResult(() -> store.tryStartProcessing("msg-1", "billing"))).isTrue();
        inTransaction(() -> store.markFailed("msg-1", "billing", "temporary failure"));
        inTransaction(() -> store.markProcessed("msg-1", "billing"));

        InboxMessage message = load("msg-1", "billing");
        assertThat(message.getStatus()).isEqualTo(InboxMessageStatus.FAILED);
        assertThat(message.getErrorMessage()).isEqualTo("temporary failure");
    }

    @Test
    void staleFailureCannotOverwriteAProcessedMessage() {
        assertThat(inTransactionResult(() -> store.tryStartProcessing("msg-1", "billing"))).isTrue();
        inTransaction(() -> store.markProcessed("msg-1", "billing"));
        inTransaction(() -> store.markFailed("msg-1", "billing", "stale failure"));

        InboxMessage message = load("msg-1", "billing");
        assertThat(message.getStatus()).isEqualTo(InboxMessageStatus.PROCESSED);
        assertThat(message.getErrorMessage()).isNull();
        assertThat(store.isProcessed("msg-1", "billing")).isTrue();
    }

    private void persist(InboxMessage message) {
        inTransaction(() -> entityManager.persist(JpaInboxMessageEntity.fromMessage(message)));
    }

    private InboxMessage load(String messageId, String consumerName) {
        return inTransactionResult(() -> {
            entityManager.clear();
            return entityManager.createQuery("""
                    select e from JpaInboxMessageEntity e
                     where e.messageId = :messageId and e.consumerName = :consumerName
                    """, JpaInboxMessageEntity.class)
                    .setParameter("messageId", messageId)
                    .setParameter("consumerName", consumerName)
                    .getSingleResult()
                    .toMessage();
        });
    }

    private long count(String messageId) {
        return inTransactionResult(() -> entityManager.createQuery("""
                select count(e) from JpaInboxMessageEntity e where e.messageId = :messageId
                """, Long.class)
                .setParameter("messageId", messageId)
                .getSingleResult());
    }

    private static InboxMessage failed(String messageId, String consumerName, String errorMessage) {
        InboxMessage message = InboxMessage.processing(messageId, consumerName);
        message.setStatus(InboxMessageStatus.FAILED);
        message.setErrorMessage(errorMessage);
        return message;
    }

    private void inTransaction(Runnable work) {
        entityManager.getTransaction().begin();
        try {
            work.run();
            entityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            entityManager.getTransaction().rollback();
            throw exception;
        }
    }

    private <T> T inTransactionResult(java.util.function.Supplier<T> work) {
        entityManager.getTransaction().begin();
        try {
            T result = work.get();
            entityManager.getTransaction().commit();
            return result;
        } catch (RuntimeException exception) {
            entityManager.getTransaction().rollback();
            throw exception;
        }
    }
}
