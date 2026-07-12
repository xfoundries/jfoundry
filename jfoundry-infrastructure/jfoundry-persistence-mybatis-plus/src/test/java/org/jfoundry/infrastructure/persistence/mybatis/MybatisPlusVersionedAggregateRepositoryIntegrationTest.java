package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrder;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderId;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderStatus;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestVersionedOrderRepository;
import org.jfoundry.infrastructure.persistence.mybatis.support.VersionedOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        PersistenceTestConfig.class,
        MybatisPlusVersionedAggregateRepositoryIntegrationTest.VersionedPluginConfiguration.class
})
@Transactional
class MybatisPlusVersionedAggregateRepositoryIntegrationTest {

    @Autowired
    private TestVersionedOrderRepository repository;

    @Autowired
    private VersionedOrderMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
    }

    @Test
    void loadModifyAndSecondModifyAdvanceTrackedVersion() {
        TestOrderId id = new TestOrderId("VERSIONED-MODIFY");
        repository.add(TestOrder.create(id, 100));
        TestOrder loaded = repository.findById(id);

        loaded.markPaid();
        repository.modify(loaded);
        loaded.cancel();
        repository.modify(loaded);

        assertThat(repository.findById(id).getStatus()).isEqualTo(TestOrderStatus.CANCELLED);
        assertThat(version(id)).isEqualTo(2L);
    }

    @Test
    void staleAggregateModifyThrowsConflict() {
        TestOrderId id = new TestOrderId("VERSIONED-STALE");
        repository.add(TestOrder.create(id, 100));
        TestOrder first = repository.findById(id);
        TestOrder stale = repository.findById(id);

        first.markPaid();
        repository.modify(first);
        stale.cancel();

        assertThatThrownBy(() -> repository.modify(stale))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("optimistic lock conflict");
        assertThat(repository.findById(id).getStatus()).isEqualTo(TestOrderStatus.PAID);
    }

    @Test
    void versionedRemoveDoesNotDeleteConcurrentlyModifiedRow() {
        TestOrderId id = new TestOrderId("VERSIONED-REMOVE");
        repository.add(TestOrder.create(id, 100));
        TestOrder loaded = repository.findById(id);
        jdbcTemplate.update("update versioned_order set version = version + 1 where id = ?", id.value());

        assertThatThrownBy(() -> repository.remove(loaded))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("optimistic lock conflict");
        assertThat(repository.findById(id)).isNotNull();
    }

    @Test
    void untrackedAggregateCannotBeModified() {
        TestOrderId id = new TestOrderId("VERSIONED-UNTRACKED");
        repository.add(TestOrder.create(id, 100));
        TestOrder untracked = TestOrder.restore(
                id, TestOrderStatus.PAID, 100,
                repository.findById(id).getCreatedAt(),
                repository.findById(id).getUpdatedAt());

        assertThatThrownBy(() -> repository.modify(untracked))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not tracked");
    }

    private Long version(TestOrderId id) {
        return jdbcTemplate.queryForObject(
                "select version from versioned_order where id = ?", Long.class, id.value());
    }

    @TestConfiguration
    static class VersionedPluginConfiguration {

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            return interceptor;
        }
    }
}
