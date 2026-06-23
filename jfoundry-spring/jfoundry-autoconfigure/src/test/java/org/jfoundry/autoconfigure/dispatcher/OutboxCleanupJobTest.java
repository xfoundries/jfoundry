package org.jfoundry.autoconfigure.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxData;
import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxMapper;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-5: cleanup job must delete PUBLISHED records older than the retention threshold,
/// and leave recent PUBLISHED records alone. Same applies to DEAD_LETTERED.
/// <p>
/// Test isolation: the dedicated H2 DB name {@code jfoundry-cleanup-test} avoids sharing
/// state with other autoconfigure tests that use {@code jfoundry-outbox-test} or
/// {@code jfoundry-starter-test}. We do NOT set {@code jfoundry.outbox.dispatcher.enabled=false}
/// because that flag disables the entire {@link OutboxDispatcherAutoConfiguration}
/// (including the {@link OutboxCleanupJob} bean we are testing). Instead, we rely on the
/// fact that all seeded records are in terminal states (PUBLISHED / DEAD_LETTERED), which
/// the scheduled dispatcher ignores — it only picks PENDING / FAILED rows. The dispatcher
/// poll interval is bumped to 10 min via {@code jfoundry.outbox.dispatcher.interval-ms}
/// so the scheduled dispatcher doesn't run during the short test window ( belt-and-
/// suspenders; terminal-state seed records wouldn't be picked anyway).
/// <p>
/// Caveat 1 (brief): {@code OutboxRepository.findById(...)} does not exist on the SPI —
/// we verify deletion through {@link OutboxMapper#selectById(String)} instead (same
/// pattern as {@code RecoverStuckDispatchingTest}).
/// <p>
/// Caveat 5 (brief): {@code OutboxEntry.newBuilder()} does not exist; the real factory is
/// {@link OutboxEntry#newPending}. After {@code append}, the row is in PENDING, so we
/// flip it to the target terminal state via {@link OutboxMapper#updateStatus}.
@SpringBootTest(classes = OutboxCleanupJobTest.TestApp.class)
@TestPropertySource(properties = {
        "jfoundry.outbox.dispatcher.interval-ms=600000",
        "jfoundry.outbox.cleanup.published-retention-days=7",
        "jfoundry.outbox.cleanup.dead-lettered-retention-days=30",
        "jfoundry.outbox.cleanup.batch-size=100",
        "spring.datasource.url=jdbc:h2:mem:jfoundry-cleanup-test;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:outbox_event.sql"
})
class OutboxCleanupJobTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.messaging.mybatis.outbox")
    static class TestApp {
        /// DomainEventExternalizerAutoConfiguration's unconditional payloadSerializer bean
        /// pulls in Jackson. Same pattern as OutboxTableNameOverrideTest /
        /// PaginationInterceptorDbTypeWiringTest.
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxMapper mapper;

    @Autowired
    private OutboxCleanupJob job;

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
    }

    @Test
    void deletesOnlyOldTerminalRecords() {
        // Seed: 1 PUBLISHED 8 days ago (older than 7-day retention → deleted),
        //       1 PUBLISHED 1 day ago (within 7-day retention → kept).
        seed("evt-old", OutboxStatus.PUBLISHED, Instant.now().minusSeconds(8 * 86400L));
        seed("evt-recent", OutboxStatus.PUBLISHED, Instant.now().minusSeconds(86400L));

        int deleted = job.runOnce();

        assertThat(deleted).isEqualTo(1);
        assertThat(mapper.selectById("evt-old")).isNull();
        assertThat(mapper.selectById("evt-recent")).isNotNull();
    }

    @Test
    void deletesDeadLetteredOlderThanRetention() {
        // DEAD_LETTERED has a longer retention (30d) than PUBLISHED (7d).
        // Seed: 1 DEAD_LETTERED 40 days ago (> 30d → deleted),
        //       1 DEAD_LETTERED 10 days ago (< 30d → kept even though > PUBLISHED retention).
        seed("evt-dead-old", OutboxStatus.DEAD_LETTERED, Instant.now().minusSeconds(40L * 86400L));
        seed("evt-dead-recent", OutboxStatus.DEAD_LETTERED, Instant.now().minusSeconds(10L * 86400L));

        int deleted = job.runOnce();

        assertThat(deleted).isEqualTo(1);
        assertThat(mapper.selectById("evt-dead-old")).isNull();
        assertThat(mapper.selectById("evt-dead-recent")).isNotNull();
    }

    @Test
    void preservesNonTerminalRecords() {
        // PENDING / DISPATCHING / FAILED records must NEVER be deleted by the cleanup job,
        // regardless of age — only terminal states (PUBLISHED / DEAD_LETTERED) are eligible.
        seed("evt-pending", OutboxStatus.PENDING, Instant.now().minusSeconds(365L * 86400L));
        seed("evt-failed", OutboxStatus.FAILED, Instant.now().minusSeconds(365L * 86400L));

        int deleted = job.runOnce();

        assertThat(deleted).isZero();
        assertThat(mapper.selectById("evt-pending")).isNotNull();
        assertThat(mapper.selectById("evt-failed")).isNotNull();
    }

    @Test
    void disabledCleanupIsNoOp() {
        // Re-fetch the properties bean and disable it at runtime (no ApplicationContext restart).
        // Validates the @Scheduled method's short-circuit guard: disabled → return 0, no Repository call.
        seed("evt-disabled", OutboxStatus.PUBLISHED, Instant.now().minusSeconds(365L * 86400L));

        // Direct call with a job whose properties.isEnabled()=false
        OutboxCleanupProperties disabled = new OutboxCleanupProperties();
        disabled.setEnabled(false);
        OutboxCleanupJob disabledJob = new OutboxCleanupJob(repository, disabled);

        int deleted = disabledJob.runOnce();

        assertThat(deleted).isZero();
        assertThat(mapper.selectById("evt-disabled")).isNotNull();
    }

    @Test
    void batchSizeLoopsUntilAllDeleted() {
        // Seed more records than batch-size; repository must loop until all matching records are gone.
        // batch-size=100 (from @TestPropertySource); seed 250 PUBLISHED records 1 year ago.
        Instant yearAgo = Instant.now().minusSeconds(365L * 86400L);
        for (int i = 0; i < 250; i++) {
            seed("evt-batch-" + i, OutboxStatus.PUBLISHED, yearAgo);
        }

        int deleted = job.runOnce();

        assertThat(deleted).isEqualTo(250);
        // Spot-check: first and last should be gone.
        assertThat(mapper.selectById("evt-batch-0")).isNull();
        assertThat(mapper.selectById("evt-batch-249")).isNull();
    }

    /// Helper: append a PENDING entry with the given occurredAt, then flip it to the target status.
    /// OutboxEntry.newPending is the only factory; status transitions via markPublished /
    /// markFailed are for the dispatcher path. For the cleanup test we bypass the state machine
    /// and set status directly via mapper.updateStatus (test-only helper).
    private void seed(String id, OutboxStatus status, Instant occurredAt) {
        OutboxEntry entry = OutboxEntry.newPending(
                id, "test.event", null, "test.type", "{}", occurredAt);
        repository.append(entry);
        mapper.updateStatus(id, status);
        // Sanity: verify the seed actually landed in the expected terminal state.
        OutboxData seeded = mapper.selectById(id);
        assertThat(seeded).as("seeded entry must exist with status=%s", status).isNotNull();
        assertThat(seeded.getStatus()).isEqualTo(status.name());
    }
}
