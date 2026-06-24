package org.jfoundry.infrastructure.outbox.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.jfoundry.application.outbox.OutboxEntry;
import org.jfoundry.application.outbox.OutboxRepository;
import org.jfoundry.application.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// P2-1 Task 2.3: {@code recoverStuckDispatching} must reset DISPATCHING records
/// whose {@code claimed_at} is older than the cutoff back to PENDING, clearing
/// {@code claimed_at}/{@code claimed_by}. Fresh DISPATCHING records (claimedAt
/// within the threshold) must remain untouched.
/// <p>
/// Test location rationale: this test exercises the MybatisPlus repository +
/// mapper stack only; {@code OutboxRecoveryJob} (autoconfig-layer scheduled
/// wrapper) is verified separately via the {@code jfoundry-autoconfigure}
/// module's existing dispatcher tests. Keeping the repo-level test in the
/// mybatis-plus module avoids the autoconfigure test module's known issue of
/// cross-test ApplicationContext caching polluting the integration test that
/// shares the same in-mem H2 DB.
/// <p>
/// Reuses {@link OutboxPersistenceTestConfig} for DataSource + mapper +
/// repository wiring (same pattern as {@code ClaimDispatchableConcurrencyTest}).
@SpringBootTest(classes = RecoverStuckDispatchingTest.TestApp.class)
class RecoverStuckDispatchingTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp extends OutboxPersistenceTestConfig {
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxMapper mapper;

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
    }

    @Test
    void stuckDispatchingRecordIsResetToPending() {
        // Seed a PENDING record and atomically claim it -> DISPATCHING.
        OutboxEntry entry = OutboxEntry.newPending(
                "evt-stuck", "test.event", null, "test.type", "{}", Instant.now());
        repository.append(entry);
        repository.claimDispatchable(1, "pod-stuck");

        // Manually age claimedAt to 10 minutes ago (simulating pod crash during DISPATCHING).
        // 走标准 lambdaUpdate（mapper 上的自定义 SQL helper 已移除，所有 UPDATE 由 BaseMapper + Wrapper 完成）。
        mapper.update(null,
                Wrappers.lambdaUpdate(OutboxData.class)
                        .set(OutboxData::getClaimedAt, Instant.now().minus(Duration.ofMinutes(10)))
                        .eq(OutboxData::getEventId, "evt-stuck"));

        // Recover with 5-minute cutoff: claimedAt=now-10m is strictly older than now-5m.
        int recovered = repository.recoverStuckDispatching(Instant.now().minus(Duration.ofMinutes(5)));

        assertThat(recovered).isEqualTo(1);

        // Verify record is back to PENDING and claim columns cleared.
        OutboxData reset = mapper.selectById("evt-stuck");
        assertThat(reset.getStatus()).isEqualTo(OutboxStatus.PENDING.name());
        assertThat(reset.getClaimedAt()).isNull();
        assertThat(reset.getClaimedBy()).isNull();
    }

    @Test
    void freshDispatchingRecordIsNotReset() {
        // Seed + claim -> DISPATCHING with claimedAt = now (fresh).
        OutboxEntry entry = OutboxEntry.newPending(
                "evt-fresh", "test.event", null, "test.type", "{}", Instant.now());
        repository.append(entry);
        repository.claimDispatchable(1, "pod-fresh");

        // Recover with 5-minute cutoff: fresh record's claimedAt is NOT older than cutoff.
        int recovered = repository.recoverStuckDispatching(Instant.now().minus(Duration.ofMinutes(5)));

        assertThat(recovered).isZero();

        OutboxData untouched = mapper.selectById("evt-fresh");
        assertThat(untouched.getStatus()).isEqualTo(OutboxStatus.DISPATCHING.name());
        assertThat(untouched.getClaimedBy()).isEqualTo("pod-fresh");
    }

    @Test
    void recoverStuckDispatchingRejectsNullCutoff() {
        assertThatThrownBy(() -> repository.recoverStuckDispatching(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cutoff");
    }
}
