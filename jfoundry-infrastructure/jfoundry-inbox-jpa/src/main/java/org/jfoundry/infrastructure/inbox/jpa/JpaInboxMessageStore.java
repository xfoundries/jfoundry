package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.EntityManager;
import org.jfoundry.application.inbox.InboxMessageStatus;
import org.jfoundry.application.inbox.InboxMessageStore;

import java.time.Instant;

/// Jakarta Persistence implementation of the Inbox persistence SPI.
public final class JpaInboxMessageStore implements InboxMessageStore {

    private final EntityManager entityManager;
    private final JpaInboxClaimStrategy claimStrategy;

    public JpaInboxMessageStore(EntityManager entityManager, JpaInboxClaimStrategy claimStrategy) {
        this.entityManager = entityManager;
        this.claimStrategy = claimStrategy;
    }

    @Override
    public boolean isProcessed(String messageId, String consumerName) {
        return entityManager.createQuery("""
                select count(e) from JpaInboxMessageEntity e
                 where e.messageId = :messageId and e.consumerName = :consumerName and e.status = :processed
                """, Long.class)
                .setParameter("messageId", messageId)
                .setParameter("consumerName", consumerName)
                .setParameter("processed", InboxMessageStatus.PROCESSED.name())
                .getSingleResult() > 0;
    }

    @Override
    public boolean tryStartProcessing(String messageId, String consumerName) {
        Instant now = Instant.now();
        int retried = entityManager.createQuery("""
                update JpaInboxMessageEntity e
                   set e.status = :processing, e.updatedAt = :now, e.errorMessage = null
                 where e.messageId = :messageId and e.consumerName = :consumerName and e.status = :failed
                """)
                .setParameter("processing", InboxMessageStatus.PROCESSING.name())
                .setParameter("now", now)
                .setParameter("messageId", messageId)
                .setParameter("consumerName", consumerName)
                .setParameter("failed", InboxMessageStatus.FAILED.name())
                .executeUpdate();
        entityManager.clear();
        if (retried == 1) {
            return true;
        }
        if (exists(messageId, consumerName)) {
            return false;
        }
        boolean claimed = claimStrategy.tryClaim(entityManager, messageId, consumerName, now);
        entityManager.flush();
        entityManager.clear();
        return claimed;
    }

    @Override
    public void markProcessed(String messageId, String consumerName) {
        Instant now = Instant.now();
        entityManager.createQuery("""
                update JpaInboxMessageEntity e
                   set e.status = :processed, e.processedAt = :now, e.updatedAt = :now, e.errorMessage = null
                 where e.messageId = :messageId and e.consumerName = :consumerName and e.status = :processing
                """)
                .setParameter("processed", InboxMessageStatus.PROCESSED.name())
                .setParameter("now", now)
                .setParameter("messageId", messageId)
                .setParameter("consumerName", consumerName)
                .setParameter("processing", InboxMessageStatus.PROCESSING.name())
                .executeUpdate();
        entityManager.clear();
    }

    @Override
    public void markFailed(String messageId, String consumerName, String errorMessage) {
        entityManager.createQuery("""
                update JpaInboxMessageEntity e
                   set e.status = :failed, e.updatedAt = :now, e.errorMessage = :errorMessage
                 where e.messageId = :messageId and e.consumerName = :consumerName and e.status = :processing
                """)
                .setParameter("failed", InboxMessageStatus.FAILED.name())
                .setParameter("now", Instant.now())
                .setParameter("errorMessage", errorMessage)
                .setParameter("messageId", messageId)
                .setParameter("consumerName", consumerName)
                .setParameter("processing", InboxMessageStatus.PROCESSING.name())
                .executeUpdate();
        entityManager.clear();
    }

    private boolean exists(String messageId, String consumerName) {
        return entityManager.createQuery("""
                select count(e) from JpaInboxMessageEntity e
                 where e.messageId = :messageId and e.consumerName = :consumerName
                """, Long.class)
                .setParameter("messageId", messageId)
                .setParameter("consumerName", consumerName)
                .getSingleResult() > 0;
    }
}
