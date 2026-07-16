package org.jfoundry.infrastructure.outbox.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.application.outbox.OutboxMessageStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaOutboxMessageStoreTest {

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private JpaOutboxMessageStore store;

    private final BackoffStrategy fixedBackoff = failedAttempts -> Duration.ofSeconds(10);

    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("jfoundry-outbox-jpa-test");
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        store = new JpaOutboxMessageStore(entityManager);
        inTransaction(() -> entityManager.createQuery("delete from JpaOutboxMessageEntity").executeUpdate());
    }

    @AfterEach
    void closeEntityManager() {
        entityManager.close();
    }

    @Test
    void claimReturnsOnlyRowsWhoseConditionalUpdateSucceededInDispatchOrder() {
        append(pending("evt-2", Instant.parse("2026-07-16T10:00:01Z")));
        append(pending("evt-1", Instant.parse("2026-07-16T10:00:00Z")));
        OutboxMessage futureRetry = pending("evt-future", Instant.parse("2026-07-16T09:59:00Z"));
        futureRetry.setStatus(OutboxMessageStatus.FAILED);
        futureRetry.setNextRetryAt(Instant.now().plusSeconds(60));
        append(futureRetry);

        List<OutboxMessage> claimed = inTransactionResult(() -> store.claimDispatchable(2, "node-a"));

        assertThat(claimed).extracting(OutboxMessage::getEventId).containsExactly("evt-1", "evt-2");
        assertThat(claimed).allSatisfy(message -> {
            assertThat(message.getStatus()).isEqualTo(OutboxMessageStatus.DISPATCHING);
            assertThat(message.getClaimedBy()).isEqualTo("node-a");
            assertThat(message.getClaimedAt()).isNotNull();
            assertThat(message.getClaimToken()).isNotBlank();
        });
        assertThat(claimed).extracting(OutboxMessage::getClaimToken).containsOnly(claimed.get(0).getClaimToken());
        assertThat(load("evt-future").getStatus()).isEqualTo(OutboxMessageStatus.FAILED);
    }

    @Test
    void findDispatchableOrdersPendingAndRetryDueRowsByOccurrenceThenEventId() {
        Instant occurredAt = Instant.parse("2026-07-16T10:00:00Z");
        append(pending("evt-b", occurredAt));
        append(pending("evt-a", occurredAt));
        OutboxMessage failedDue = pending("evt-retry", occurredAt.plusSeconds(1));
        failedDue.setStatus(OutboxMessageStatus.FAILED);
        failedDue.setNextRetryAt(Instant.now().minusSeconds(1));
        append(failedDue);

        List<OutboxMessage> dispatchable = inTransactionResult(() -> store.findDispatchable(3, Instant.now()));

        assertThat(dispatchable).extracting(OutboxMessage::getEventId)
                .containsExactly("evt-a", "evt-b", "evt-retry");
    }

    @Test
    void staleClaimTokenCannotPublishOrFailAClaimedRow() {
        append(pending("evt-1", Instant.now()));
        OutboxMessage claim = inTransactionResult(() -> store.claimDispatchable(1, "node-a").get(0));

        inTransaction(() -> store.markAsPublished("evt-1", "wrong-token"));
        assertThat(load("evt-1").getStatus()).isEqualTo(OutboxMessageStatus.DISPATCHING);
        inTransaction(() -> store.markAsFailed("evt-1", "wrong-token", "boom", 3, fixedBackoff));
        assertThat(load("evt-1").getStatus()).isEqualTo(OutboxMessageStatus.DISPATCHING);

        inTransaction(() -> store.markAsPublished("evt-1", claim.getClaimToken()));
        OutboxMessage published = load("evt-1");
        assertThat(published.getStatus()).isEqualTo(OutboxMessageStatus.PUBLISHED);
        assertThat(published.getClaimedAt()).isNull();
        assertThat(published.getClaimedBy()).isNull();
        assertThat(published.getClaimToken()).isNull();
    }

    @Test
    void failureRetryDeadLetterAndReactivationFollowTheCoreStateMachine() {
        append(pending("evt-1", Instant.now()));
        OutboxMessage firstClaim = inTransactionResult(() -> store.claimDispatchable(1, "node-a").get(0));

        inTransaction(() -> store.markAsFailed("evt-1", firstClaim.getClaimToken(), "boom", 2, fixedBackoff));
        OutboxMessage failed = load("evt-1");
        assertThat(failed.getStatus()).isEqualTo(OutboxMessageStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getNextRetryAt()).isAfter(failed.getLastAttemptAt());
        assertThat(failed.getClaimToken()).isNull();

        inTransaction(() -> entityManager.createQuery("update JpaOutboxMessageEntity e set e.nextRetryAt = :now")
                .setParameter("now", Instant.now().minusSeconds(1)).executeUpdate());
        OutboxMessage secondClaim = inTransactionResult(() -> store.claimDispatchable(1, "node-b").get(0));
        inTransaction(() -> store.markAsFailed("evt-1", secondClaim.getClaimToken(), "boom again", 2, fixedBackoff));
        assertThat(load("evt-1").getStatus()).isEqualTo(OutboxMessageStatus.DEAD_LETTERED);

        inTransaction(() -> store.reactivate("evt-1"));
        OutboxMessage reactivated = load("evt-1");
        assertThat(reactivated.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(reactivated.getRetryCount()).isZero();
        assertThat(reactivated.getErrorMessage()).isNull();
    }

    @Test
    void recoveryClearsOnlyOwnershipOlderThanTheCutoff() {
        append(pending("old", Instant.now()));
        append(pending("fresh", Instant.now()));
        inTransaction(() -> store.claimDispatchable(2, "node-a"));
        inTransaction(() -> entityManager.createQuery("update JpaOutboxMessageEntity e set e.claimedAt = :old where e.eventId = :eventId")
                .setParameter("old", Instant.now().minusSeconds(120))
                .setParameter("eventId", "old").executeUpdate());

        int recovered = inTransactionResult(() -> store.recoverStuckDispatching(Instant.now().minusSeconds(60)));

        assertThat(recovered).isEqualTo(1);
        OutboxMessage old = load("old");
        assertThat(old.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(old.getClaimedAt()).isNull();
        assertThat(old.getClaimedBy()).isNull();
        assertThat(old.getClaimToken()).isNull();
        assertThat(load("fresh").getStatus()).isEqualTo(OutboxMessageStatus.DISPATCHING);
    }

    @Test
    void cleanupDeletesBoundedPagesForBothTerminalStatuses() {
        Instant old = Instant.now().minusSeconds(120);
        append(terminal("published-1", OutboxMessageStatus.PUBLISHED, old));
        append(terminal("published-2", OutboxMessageStatus.PUBLISHED, old));
        append(terminal("dead-1", OutboxMessageStatus.DEAD_LETTERED, old));
        append(terminal("recent", OutboxMessageStatus.PUBLISHED, Instant.now()));

        assertThat(inTransactionResult(() -> store.deleteByStatusAndOccurredAtBefore(
                OutboxMessageStatus.PUBLISHED, Instant.now().minusSeconds(60), 1))).isEqualTo(2);
        assertThat(inTransactionResult(() -> store.deleteByStatusAndOccurredAtBefore(
                OutboxMessageStatus.DEAD_LETTERED, Instant.now().minusSeconds(60), 1))).isEqualTo(1);
        assertThat(load("published-1")).isNull();
        assertThat(load("published-2")).isNull();
        assertThat(load("dead-1")).isNull();
        assertThat(load("recent")).isNotNull();
    }

    @Test
    void validatesClaimRecoveryAndCleanupInputs() {
        assertThatThrownBy(() -> store.claimDispatchable(0, "node-a"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("limit");
        assertThatThrownBy(() -> store.claimDispatchable(1, " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("claimerId");
        assertThatThrownBy(() -> store.recoverStuckDispatching(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cutoff");
        assertThatThrownBy(() -> store.deleteByStatusAndOccurredAtBefore(null, Instant.now(), 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("status");
        assertThatThrownBy(() -> store.deleteByStatusAndOccurredAtBefore(
                OutboxMessageStatus.PUBLISHED, null, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cutoff");
        assertThatThrownBy(() -> store.deleteByStatusAndOccurredAtBefore(
                OutboxMessageStatus.PUBLISHED, Instant.now(), 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("batchSize");
    }

    private void append(OutboxMessage message) {
        inTransaction(() -> store.append(message));
    }

    private OutboxMessage load(String eventId) {
        return inTransactionResult(() -> {
            JpaOutboxMessageEntity entity = entityManager.find(JpaOutboxMessageEntity.class, eventId);
            return entity == null ? null : entity.toMessage();
        });
    }

    private static OutboxMessage pending(String eventId, Instant occurredAt) {
        return OutboxMessage.newPending(eventId, "topic", null, "example.Event", "{}", occurredAt);
    }

    private static OutboxMessage terminal(String eventId, OutboxMessageStatus status, Instant occurredAt) {
        OutboxMessage message = pending(eventId, occurredAt);
        message.setStatus(status);
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
