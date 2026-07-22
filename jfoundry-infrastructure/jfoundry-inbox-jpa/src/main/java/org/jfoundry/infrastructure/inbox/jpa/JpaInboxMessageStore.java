package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.EntityManager;
import org.jfoundry.application.inbox.InboxMessageStatus;
import org.jfoundry.application.inbox.InboxMessageStore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
        int retried = entityManager.createNativeQuery("""
                update jfoundry_inbox_message
                   set status = ?1, updated_at = ?2, error_message = null
                 where message_id = ?3 and consumer_name = ?4 and status = ?5
                """)
                .setParameter(1, InboxMessageStatus.PROCESSING.name())
                .setParameter(2, utcTimestamp(now))
                .setParameter(3, messageId)
                .setParameter(4, consumerName)
                .setParameter(5, InboxMessageStatus.FAILED.name())
                .executeUpdate();
        entityManager.flush();
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
        entityManager.createNativeQuery("""
                update jfoundry_inbox_message
                   set status = ?1, processed_at = ?2, updated_at = ?2, error_message = null
                 where message_id = ?3 and consumer_name = ?4 and status = ?5
                """)
                .setParameter(1, InboxMessageStatus.PROCESSED.name())
                .setParameter(2, utcTimestamp(now))
                .setParameter(3, messageId)
                .setParameter(4, consumerName)
                .setParameter(5, InboxMessageStatus.PROCESSING.name())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public void markFailed(String messageId, String consumerName, String errorMessage) {
        entityManager.createNativeQuery("""
                update jfoundry_inbox_message
                   set status = ?1, updated_at = ?2, error_message = ?3
                 where message_id = ?4 and consumer_name = ?5 and status = ?6
                """)
                .setParameter(1, InboxMessageStatus.FAILED.name())
                .setParameter(2, utcTimestamp(Instant.now()))
                .setParameter(3, errorMessage)
                .setParameter(4, messageId)
                .setParameter(5, consumerName)
                .setParameter(6, InboxMessageStatus.PROCESSING.name())
                .executeUpdate();
        entityManager.flush();
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

    private static LocalDateTime utcTimestamp(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
