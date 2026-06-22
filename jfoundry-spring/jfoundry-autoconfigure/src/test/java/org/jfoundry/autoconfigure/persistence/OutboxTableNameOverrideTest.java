package org.jfoundry.autoconfigure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-2: {@code jfoundry.outbox.table-name} must redirect OutboxData persistence to a
/// custom-named physical table. The {@link OutboxMybatisPlusAutoConfiguration} registers
/// a {@code DynamicTableNameInnerInterceptor} that rewrites the logical name
/// {@code ddd_outbox_event} to whatever business configures.
/// <p>
/// Side assertion: the default {@code ddd_outbox_event} table (also created by the test
/// fixture) must stay empty — proving the rewrite happened, not that we widened the write.
/// <p>
/// Isolation: this test brings up the full autoconfig chain, so we (1) disable the
/// outbox dispatcher to avoid its polling interacting with downstream tests, and (2) use
/// a dedicated in-memory H2 name. Same cross-test flakiness pattern as Task 2.3 (see
/// {@code task-2.3-report.md}) — Task 2.3 moved its test to messaging-mybatis-plus; we
/// keep this one in autoconfigure because it specifically exercises the
/// autoconfig-layer {@code TableNameHandler} wiring.
@SpringBootTest(
        classes = OutboxTableNameOverrideTest.TestApp.class,
        properties = {
                "jfoundry.outbox.table-name=custom_outbox",
                // Disable the outbox dispatcher so this test exercises only the persistence
                // layer (append → TableNameHandler → custom_outbox). Otherwise the full
                // autoconfig chain starts a ScheduledOutboxDispatcher whose polling may
                // interact with subsequent tests sharing the same H2 instance.
                "jfoundry.outbox.dispatcher.enabled=false",
                // Dedicated in-memory DB name for isolation from DomainEventExternalizationIntegrationTest.
                "spring.datasource.url=jdbc:h2:mem:jfoundry-table-name-override;DB_CLOSE_DELAY=-1",
                "spring.sql.init.schema-locations=classpath:outbox_event.sql"
        }
)
@Sql(scripts = "classpath:outbox_custom_table.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OutboxTableNameOverrideTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.messaging.mybatis.outbox")
    static class TestApp {
        /// DomainEventExternalizerAutoConfiguration's unconditional payloadSerializer bean
        /// pulls in Jackson. Same pattern as OutboxDispatcherEnabledTest / DomainEventExternalizationIntegrationTest.
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appendWritesToCustomTable() {
        OutboxEntry entry = OutboxEntry.newPending(
                "evt-custom", "test.event", null, "test.type", "{}", Instant.now());
        repository.append(entry);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE event_id = ?",
                Integer.class, "evt-custom");
        assertThat(count).isEqualTo(1);

        Integer defaultCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ddd_outbox_event WHERE event_id = ?",
                Integer.class, "evt-custom");
        assertThat(defaultCount)
                .as("default table must be empty when table-name override is set")
                .isEqualTo(0);
    }
}
