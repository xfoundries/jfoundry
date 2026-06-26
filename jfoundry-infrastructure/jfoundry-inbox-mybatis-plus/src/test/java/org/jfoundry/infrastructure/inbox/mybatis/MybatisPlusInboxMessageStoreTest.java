package org.jfoundry.infrastructure.inbox.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InboxPersistenceTestConfig.class)
class MybatisPlusInboxMessageStoreTest {

    @Autowired
    private MybatisPlusInboxMessageStore store;

    @Autowired
    private InboxMessageMapper mapper;

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
    }

    @Test
    void markProcessedPersistsProcessedMessage() {
        store.markProcessed("evt-1", "projection");

        assertThat(mapper.selectCount(null)).isEqualTo(1);
        InboxMessageData data = mapper.selectOne(Wrappers.lambdaQuery(InboxMessageData.class)
                .eq(InboxMessageData::getMessageId, "evt-1")
                .eq(InboxMessageData::getConsumerName, "projection"));
        assertThat(data.getId()).isNotBlank();
    }

    @Test
    void isProcessedReturnsTrueForExistingProcessedMessage() {
        store.markProcessed("evt-1", "projection");

        assertThat(store.isProcessed("evt-1", "projection")).isTrue();
    }

    @Test
    void duplicateMarkProcessedIsIdempotent() {
        store.markProcessed("evt-1", "projection");
        store.markProcessed("evt-1", "projection");

        assertThat(mapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void differentConsumersCanProcessSameMessage() {
        store.markProcessed("evt-1", "projection-a");
        store.markProcessed("evt-1", "projection-b");

        assertThat(store.isProcessed("evt-1", "projection-a")).isTrue();
        assertThat(store.isProcessed("evt-1", "projection-b")).isTrue();
        assertThat(mapper.selectCount(null)).isEqualTo(2);
    }

    @Test
    void concurrentProcessingExecutesHandlerOnlyOnce() throws Exception {
        org.jfoundry.application.inbox.InboxTemplate template =
                new org.jfoundry.application.inbox.InboxTemplate(store);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch entered = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Runnable task = () -> {
                try {
                    start.await();
                    template.executeOnce("evt-1", "projection", () -> {
                        calls.incrementAndGet();
                        entered.countDown();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            pool.submit(task);
            pool.submit(task);

            start.countDown();

            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(calls.get()).isEqualTo(1);
        assertThat(store.isProcessed("evt-1", "projection")).isTrue();
        assertThat(mapper.selectCount(null)).isEqualTo(1);
    }
}
