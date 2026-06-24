package org.jfoundry.infrastructure.outbox.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.OutboxEntry;
import org.jfoundry.infrastructure.outbox.core.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OutboxPersistenceTestConfig.class)
class MybatisPlusOutboxRepositoryTest {

    @Autowired
    private MybatisPlusOutboxRepository repository;

    private final BackoffStrategy fixedBackoff = failedAttempts -> Duration.ofSeconds(10);

    @BeforeEach
    void cleanDb(@Autowired OutboxMapper mapper) {
        mapper.delete(null);
    }

    @Test
    void appendPersistsEntry() {
        OutboxEntry entry = pendingEntry("evt-1");
        repository.append(entry);

        OutboxEntry loaded = repository.findDispatchable(100, Instant.now()).get(0);
        assertThat(loaded.getEventId()).isEqualTo("evt-1");
    }

    @Test
    void appendPersistsAggregateRoutingMetadata() {
        OutboxEntry entry = OutboxEntry.newPending(
                "evt-aggregate", "topic", null, "com.example.Foo", "{}", Instant.now(),
                "Order", "order-1", 7L);

        repository.append(entry);

        OutboxEntry loaded = repository.findDispatchable(100, Instant.now()).get(0);
        assertThat(loaded.getAggregateType()).isEqualTo("Order");
        assertThat(loaded.getAggregateId()).isEqualTo("order-1");
        assertThat(loaded.getAggregateVersion()).isEqualTo(7L);
    }

    @Test
    void findDispatchableReturnsOnlyPendingOrFailedReady() {
        repository.append(pendingEntry("evt-ready"));
        OutboxEntry failedNotReady = pendingEntry("evt-failed-not-ready");
        failedNotReady.markFailed("err", 5, fixedBackoff); // nextRetryAt = now + 10s
        repository.append(failedNotReady);

        List<OutboxEntry> ready = repository.findDispatchable(100, Instant.now());

        assertThat(ready).extracting(OutboxEntry::getEventId).containsExactly("evt-ready");
    }

    @Test
    void findDispatchableReturnsFailedWhoseNextRetryAtReached() throws InterruptedException {
        BackoffStrategy instant = failedAttempts -> Duration.ofMillis(1);
        OutboxEntry failed = pendingEntry("evt-failed");
        failed.markFailed("err", 5, instant);
        repository.append(failed);

        Thread.sleep(20); // wait for next_retry_at to pass

        List<OutboxEntry> ready = repository.findDispatchable(100, Instant.now());
        assertThat(ready).extracting(OutboxEntry::getEventId).contains("evt-failed");
    }

    @Test
    void markAsPublishedTransitionsToPublished() {
        repository.append(pendingEntry("evt-1"));
        repository.markAsPublished("evt-1");

        List<OutboxEntry> ready = repository.findDispatchable(100, Instant.now());
        assertThat(ready).isEmpty();
    }

    @Test
    void markAsFailedIncrementsRetryAndSetsNextRetryAt() {
        repository.append(pendingEntry("evt-1"));

        repository.markAsFailed("evt-1", "boom", 5, fixedBackoff);

        // Should not be dispatchable immediately (nextRetryAt = now + 10s)
        List<OutboxEntry> ready = repository.findDispatchable(100, Instant.now());
        assertThat(ready).isEmpty();
    }

    @Test
    void markAsFailedTransitionsToDeadLetteredAtMaxRetries() {
        repository.append(pendingEntry("evt-1"));

        repository.markAsFailed("evt-1", "boom", 1, fixedBackoff);

        // DEAD_LETTERED should not be dispatchable
        assertThat(repository.findDispatchable(100, Instant.now())).isEmpty();
    }

    @Test
    void reactivateResetsDeadLetteredToPending() {
        repository.append(pendingEntry("evt-1"));
        repository.markAsFailed("evt-1", "boom", 1, fixedBackoff);

        repository.reactivate("evt-1");

        List<OutboxEntry> ready = repository.findDispatchable(100, Instant.now());
        assertThat(ready).extracting(OutboxEntry::getEventId).contains("evt-1");
    }

    @Test
    void reactivateRejectsNonDeadLettered() {
        repository.append(pendingEntry("evt-1"));

        assertThatThrownBy(() -> repository.reactivate("evt-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEAD_LETTERED");
    }

    @Test
    void markAsPublishedMissingEntryIsSilent() {
        // No exception thrown
        repository.markAsPublished("does-not-exist");
    }

    private OutboxEntry pendingEntry(String eventId) {
        return OutboxEntry.newPending(
                eventId, "topic", null, "com.example.Foo", "{}", Instant.now());
    }
}
