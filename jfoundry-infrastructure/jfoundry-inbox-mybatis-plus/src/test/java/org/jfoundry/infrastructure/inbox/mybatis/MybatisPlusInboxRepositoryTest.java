package org.jfoundry.infrastructure.inbox.mybatis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InboxPersistenceTestConfig.class)
class MybatisPlusInboxRepositoryTest {

    @Autowired
    private MybatisPlusInboxRepository repository;

    @Autowired
    private InboxMessageMapper mapper;

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
    }

    @Test
    void markProcessedPersistsProcessedMessage() {
        repository.markProcessed("evt-1", "projection");

        assertThat(mapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void isProcessedReturnsTrueForExistingProcessedMessage() {
        repository.markProcessed("evt-1", "projection");

        assertThat(repository.isProcessed("evt-1", "projection")).isTrue();
    }

    @Test
    void duplicateMarkProcessedIsIdempotent() {
        repository.markProcessed("evt-1", "projection");
        repository.markProcessed("evt-1", "projection");

        assertThat(mapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void differentConsumersCanProcessSameMessage() {
        repository.markProcessed("evt-1", "projection-a");
        repository.markProcessed("evt-1", "projection-b");

        assertThat(repository.isProcessed("evt-1", "projection-a")).isTrue();
        assertThat(repository.isProcessed("evt-1", "projection-b")).isTrue();
        assertThat(mapper.selectCount(null)).isEqualTo(2);
    }
}
