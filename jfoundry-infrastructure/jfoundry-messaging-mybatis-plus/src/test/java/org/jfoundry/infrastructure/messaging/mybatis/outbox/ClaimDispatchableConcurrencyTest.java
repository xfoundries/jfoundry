package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// P2-1: claimDispatchable must be atomic across threads — no two claimers
/// receive the same record.
@SpringBootTest(classes = ClaimDispatchableConcurrencyTest.TestApp.class)
class ClaimDispatchableConcurrencyTest {

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
    void twoConcurrentClaimersGetDisjointRecords() throws Exception {
        // Seed 20 PENDING records
        for (int i = 0; i < 20; i++) {
            OutboxEntry e = OutboxEntry.newPending(
                    "evt-" + i, "test.event", null, "test.type", "{}", Instant.now());
            repository.append(e);
        }

        Set<String> claimedByA = ConcurrentHashMap.newKeySet();
        Set<String> claimedByB = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        pool.submit(() -> {
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            repository.claimDispatchable(10, "pod-A").forEach(e -> claimedByA.add(e.getEventId()));
            done.countDown();
        });
        pool.submit(() -> {
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            repository.claimDispatchable(10, "pod-B").forEach(e -> claimedByB.add(e.getEventId()));
            done.countDown();
        });

        start.countDown();
        done.await();
        pool.shutdown();

        // Assertions
        assertThat(claimedByA).hasSize(10);
        assertThat(claimedByB).hasSize(10);

        Set<String> intersection = new HashSet<>(claimedByA);
        intersection.retainAll(claimedByB);
        assertThat(intersection)
                .as("claimDispatchable must be atomic: zero intersection between two claimers")
                .isEmpty();
    }

    @Test
    void claimDispatchableOnlyTakesPendingRecords() {
        // Seed: 3 PENDING, 1 DISPATCHING, 1 PUBLISHED
        repository.append(pendingEntry("pending-1"));
        repository.append(pendingEntry("pending-2"));
        repository.append(pendingEntry("pending-3"));

        OutboxEntry dispatching = pendingEntry("dispatching-1");
        dispatching.setStatus(OutboxStatus.DISPATCHING);
        repository.append(dispatching);

        OutboxEntry published = pendingEntry("published-1");
        published.setStatus(OutboxStatus.PUBLISHED);
        repository.append(published);

        List<OutboxEntry> claimed = repository.claimDispatchable(10, "pod-X");

        assertThat(claimed).extracting(OutboxEntry::getEventId)
                .containsExactlyInAnyOrder("pending-1", "pending-2", "pending-3");
    }

    @Test
    void claimDispatchableUpdatesStatusToDispatching() {
        repository.append(pendingEntry("evt-1"));

        List<OutboxEntry> claimed = repository.claimDispatchable(10, "pod-X");

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getStatus()).isEqualTo(OutboxStatus.DISPATCHING);
        assertThat(claimed.get(0).getClaimedBy()).isEqualTo("pod-X");
        assertThat(claimed.get(0).getClaimedAt()).isNotNull();
    }

    @Test
    void claimDispatchableReturnsEmptyWhenNoPending() {
        List<OutboxEntry> claimed = repository.claimDispatchable(10, "pod-X");
        assertThat(claimed).isEmpty();
    }

    @Test
    void claimDispatchableRejectsNonPositiveLimit() {
        assertThatThrownBy(() -> repository.claimDispatchable(0, "pod-X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> repository.claimDispatchable(-1, "pod-X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void claimDispatchableRejectsBlankClaimerId() {
        assertThatThrownBy(() -> repository.claimDispatchable(10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("claimerId");
        assertThatThrownBy(() -> repository.claimDispatchable(10, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("claimerId");
    }

    private OutboxEntry pendingEntry(String eventId) {
        return OutboxEntry.newPending(
                eventId, "topic", null, "com.example.Foo", "{}", Instant.now());
    }
}
