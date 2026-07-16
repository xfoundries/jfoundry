package org.jfoundry.integration.support;

import jakarta.persistence.EntityManager;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxClaimStrategies;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxClaimStrategy;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxMessageEntity;
import org.jfoundry.infrastructure.inbox.jpa.JpaInboxMessageStore;
import org.jfoundry.infrastructure.outbox.jpa.JpaOutboxMessageEntity;
import org.jfoundry.infrastructure.outbox.jpa.JpaOutboxMessageStore;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.SQLException;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {JpaOutboxMessageEntity.class, JpaInboxMessageEntity.class})
public class JpaOutboxInboxDatabaseConfig {

    @Bean
    JpaOutboxMessageStore jpaOutboxMessageStore(EntityManager entityManager) {
        return new JpaOutboxMessageStore(entityManager);
    }

    @Bean
    JpaInboxClaimStrategy jpaInboxClaimStrategy(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            return JpaInboxClaimStrategies.forProductName(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to determine database product for JpaInboxClaimStrategy", exception);
        }
    }

    @Bean
    JpaInboxMessageStore jpaInboxMessageStore(EntityManager entityManager, JpaInboxClaimStrategy claimStrategy) {
        return new JpaInboxMessageStore(entityManager, claimStrategy);
    }
}
