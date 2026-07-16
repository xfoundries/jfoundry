package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.EntityManager;
import org.jfoundry.application.inbox.InboxMessageStatus;

import java.time.Instant;
import java.util.UUID;

/// Claims Inbox messages using MySQL's duplicate-tolerant insert syntax.
public final class MySqlJpaInboxClaimStrategy implements JpaInboxClaimStrategy {

    @Override
    public boolean tryClaim(EntityManager entityManager, String messageId, String consumerName, Instant now) {
        return entityManager.createNativeQuery("""
                insert ignore into jfoundry_inbox_message
                    (id, message_id, consumer_name, status, created_at, updated_at)
                values (?1, ?2, ?3, ?4, ?5, ?6)
                """)
                .setParameter(1, UUID.randomUUID().toString())
                .setParameter(2, messageId)
                .setParameter(3, consumerName)
                .setParameter(4, InboxMessageStatus.PROCESSING.name())
                .setParameter(5, now)
                .setParameter(6, now)
                .executeUpdate() == 1;
    }
}
