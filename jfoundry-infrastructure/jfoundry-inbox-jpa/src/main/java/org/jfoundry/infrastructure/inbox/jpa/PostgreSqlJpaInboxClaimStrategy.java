package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.EntityManager;
import org.jfoundry.application.inbox.InboxMessageStatus;

import java.time.Instant;
import java.util.UUID;

/// Claims Inbox messages using PostgreSQL's conflict-safe insert syntax.
public final class PostgreSqlJpaInboxClaimStrategy implements JpaInboxClaimStrategy {

    @Override
    public boolean tryClaim(EntityManager entityManager, String messageId, String consumerName, Instant now) {
        return entityManager.createNativeQuery("""
                insert into jfoundry_inbox_message
                    (id, message_id, consumer_name, status, created_at, updated_at)
                values (?1, ?2, ?3, ?4, ?5, ?6)
                on conflict (consumer_name, message_id) do nothing
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
