package org.jfoundry.infrastructure.inbox.mybatis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}
