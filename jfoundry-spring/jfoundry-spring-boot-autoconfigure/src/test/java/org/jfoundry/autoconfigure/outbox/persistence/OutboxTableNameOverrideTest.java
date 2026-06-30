package org.jfoundry.autoconfigure.outbox.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.application.outbox.OutboxMessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-2: {@code jfoundry.outbox.table-name} must redirect OutboxData persistence to a
/// custom-named physical table. The {@link OutboxMybatisPlusAutoConfiguration} registers
/// a {@code DynamicTableNameInnerInterceptor} that rewrites the logical name
/// {@code jfoundry_outbox_event} to whatever business configures.
/// <p>
/// Side assertion: the default {@code jfoundry_outbox_event} table (also created by the test
/// fixture) must stay empty — proving the rewrite happened, not that we widened the write.
/// <p>
/// P3-3: 之前只覆盖 append；现在补 claimDispatchable / recoverStuckDispatching /
/// deleteByStatusAndOccurredAtBefore 三个运营路径，确保 {@code TableNameHandler} 改写
/// 对自定义 SQL（mapper 上的 {@code @Update}/{@code @Select}/{@code @Delete}）也生效，
/// 不只是 BaseMapper 的标准 CRUD。
/// <p>
/// Isolation: this test brings up the full autoconfig chain, so we (1) set dispatcher
/// mode to none to avoid polling interacting with downstream tests, and (2) use
/// a dedicated in-memory H2 name. Same cross-test flakiness pattern as Task 2.3 (see
/// {@code task-2.3-report.md}) — Task 2.3 moved its test to messaging-mybatis-plus; we
/// keep this one in autoconfigure because it specifically exercises the
/// autoconfig-layer {@code TableNameHandler} wiring.
@SpringBootTest(
        classes = OutboxTableNameOverrideTest.TestApp.class,
        properties = {
                "jfoundry.outbox.table-name=custom_outbox",
                // Set dispatcher mode to none so this test exercises only the persistence
                // layer (append → TableNameHandler → custom_outbox). Otherwise the full
                // autoconfig chain starts a ScheduledOutboxDispatcher whose polling may
                // interact with subsequent tests sharing the same H2 instance.
                "jfoundry.outbox.dispatcher.mode=none",
                "spring.autoconfigure.exclude=org.jfoundry.autoconfigure.outbox.dispatcher.OutboxDispatcherAutoConfiguration",
                // Dedicated in-memory DB name for isolation from DomainEventExternalizationIntegrationTest.
                "spring.datasource.url=jdbc:h2:mem:jfoundry-table-name-override;DB_CLOSE_DELAY=-1",
                "spring.sql.init.schema-locations=classpath:outbox_event.sql"
        }
)
@Sql(scripts = "classpath:outbox_custom_table.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OutboxTableNameOverrideTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.outbox.mybatis")
    static class TestApp {
        /// DomainEventOutboxRecorderAutoConfiguration's payloadSerializer bean
        /// pulls in Jackson. Same pattern as OutboxDispatcherEnabledTest / DomainEventExternalizationIntegrationTest.
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private OutboxMessageStore repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        // DB_CLOSE_DELAY=-1 让 H2 跨测试保留数据，必须 @BeforeEach 清空两张表，
        // 否则 appendWritesToCustomTable 写入的 evt-custom 会污染下一个测试的 claim/assertion。
        jdbc.update("DELETE FROM custom_outbox");
        jdbc.update("DELETE FROM jfoundry_outbox_event");
    }

    @Test
    void appendWritesToCustomTable() {
        OutboxMessage entry = OutboxMessage.newPending(
                "evt-custom", "test.event", null, "test.type", "{}", Instant.now());
        repository.append(entry);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE event_id = ?",
                Integer.class, "evt-custom");
        assertThat(count).isEqualTo(1);

        Integer defaultCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM jfoundry_outbox_event WHERE event_id = ?",
                Integer.class, "evt-custom");
        assertThat(defaultCount)
                .as("default table must be empty when table-name override is set")
                .isEqualTo(0);
    }

    /// P3-3 regression: claimDispatchable 的自定义 SQL（{@code UPDATE...LIMIT} + 后续 SELECT）
    /// 也必须被 {@code DynamicTableNameInnerInterceptor} 改写到 custom_outbox，否则会把
    /// 流量打到默认表，造成"claim 不到任何记录"的 silent failure。
    @Test
    void claimDispatchableReadsFromCustomTable() {
        appendPending("evt-claim-1", "custom_outbox");
        appendPending("evt-claim-2", "custom_outbox");

        List<OutboxMessage> claimed = repository.claimDispatchable(10, "pod-custom");

        assertThat(claimed).extracting(OutboxMessage::getEventId)
                .containsExactlyInAnyOrder("evt-claim-1", "evt-claim-2");

        // 两端确认：custom_outbox 里 status 已变 DISPATCHING；jfoundry_outbox_event 仍空。
        Integer dispatchingInCustom = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE status = 'DISPATCHING'",
                Integer.class);
        assertThat(dispatchingInCustom).isEqualTo(2);

        Integer anyInDefault = jdbc.queryForObject(
                "SELECT COUNT(*) FROM jfoundry_outbox_event",
                Integer.class);
        assertThat(anyInDefault)
                .as("claim 的 UPDATE/SELECT 也必须走 custom_outbox，默认表保持空")
                .isEqualTo(0);
    }

    /// P3-3 regression: recoverStuckDispatching 的 {@code UPDATE...WHERE status='DISPATCHING'
    /// AND claimed_at < cutoff} 必须改写到 custom_outbox，否则 recovery job 静默失效。
    @Test
    void recoverStuckDispatchingOperatesOnCustomTable() {
        // 在 custom_outbox 上构造一条陈旧 DISPATCHING 记录。
        appendPending("evt-stuck", "custom_outbox");
        repository.claimDispatchable(1, "pod-stuck");
        // 直接 age claimed_at 到 10 分钟前，模拟 pod 崩溃后的陈旧 claim。
        jdbc.update(
                "UPDATE custom_outbox SET claimed_at = ? WHERE event_id = ?",
                Instant.now().minus(Duration.ofMinutes(10)), "evt-stuck");

        int recovered = repository.recoverStuckDispatching(Instant.now().minus(Duration.ofMinutes(5)));

        assertThat(recovered).isEqualTo(1);

        String status = jdbc.queryForObject(
                "SELECT status FROM custom_outbox WHERE event_id = ?",
                String.class, "evt-stuck");
        assertThat(status).isEqualTo(OutboxMessageStatus.PENDING.name());

        // 默认表从头到尾没有任何流量。
        Integer anyInDefault = jdbc.queryForObject(
                "SELECT COUNT(*) FROM jfoundry_outbox_event",
                Integer.class);
        assertThat(anyInDefault).isEqualTo(0);
    }

    /// P3-3 regression: deleteByStatusAndOccurredAtBefore 的子查询 + LIMIT 形态 DELETE
    /// 必须改写到 custom_outbox，否则 cleanup job 静默失效（默认表本来就空，DELETE 返回 0
    /// 但不报错，最隐蔽）。
    @Test
    void deleteByStatusAndOccurredAtBeforeOperatesOnCustomTable() {
        // 构造一条"已 PUBLISHED 且 occurred_at 早于 cutoff"的记录，命中清理条件。
        appendPending("evt-published-old", "custom_outbox");
        // 直接 SQL 改 status（绕过 markPublished 的状态机；本测试只关心 DELETE 改写）。
        jdbc.update(
                "UPDATE custom_outbox SET status = ?, occurred_at = ? WHERE event_id = ?",
                OutboxMessageStatus.PUBLISHED.name(),
                Instant.now().minus(Duration.ofDays(10)),
                "evt-published-old");
        // 一条 PUBLISHED 但新鲜的记录，不应被清理。
        appendPending("evt-published-fresh", "custom_outbox");
        jdbc.update(
                "UPDATE custom_outbox SET status = ? WHERE event_id = ?",
                OutboxMessageStatus.PUBLISHED.name(), "evt-published-fresh");

        int deleted = repository.deleteByStatusAndOccurredAtBefore(
                OutboxMessageStatus.PUBLISHED, Instant.now().minus(Duration.ofDays(1)), 10);

        assertThat(deleted).isEqualTo(1);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE event_id = ?",
                Integer.class, "evt-published-old");
        assertThat(remaining).isEqualTo(0);

        Integer freshRemaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE event_id = ?",
                Integer.class, "evt-published-fresh");
        assertThat(freshRemaining)
                .as("fresh PUBLISHED 记录不应被清理")
                .isEqualTo(1);

        Integer anyInDefault = jdbc.queryForObject(
                "SELECT COUNT(*) FROM jfoundry_outbox_event",
                Integer.class);
        assertThat(anyInDefault).isEqualTo(0);
    }

    private void appendPending(String eventId, String table) {
        OutboxMessage entry = OutboxMessage.newPending(
                eventId, "test.event", null, "test.type", "{}", Instant.now());
        repository.append(entry);
    }
}
