package org.jfoundry.infrastructure.inbox.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaInboxClaimStrategiesTest {

    @Test
    void selectsOnlyBuiltInProducts() {
        assertThat(JpaInboxClaimStrategies.forProductName("PostgreSQL 16.4"))
                .isInstanceOf(PostgreSqlJpaInboxClaimStrategy.class);
        assertThat(JpaInboxClaimStrategies.forProductName("MySQL"))
                .isInstanceOf(MySqlJpaInboxClaimStrategy.class);
    }

    @Test
    void rejectsUnsupportedProductWithOverrideInstruction() {
        assertThatThrownBy(() -> JpaInboxClaimStrategies.forProductName("H2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported database product: H2")
                .hasMessageContaining("JpaInboxClaimStrategy");
    }

    @Test
    void postgresUsesNoConflictInsertAndReturnsWhetherTheRowWasInserted() {
        RecordedNativeQuery recorded = new RecordedNativeQuery(1);

        boolean claimed = new PostgreSqlJpaInboxClaimStrategy()
                .tryClaim(recorded.entityManager(), "msg-1", "billing", Instant.parse("2026-07-16T10:15:30Z"));

        assertThat(claimed).isTrue();
        assertThat(recorded.sql()).containsIgnoringCase("insert into jfoundry_inbox_message")
                .containsIgnoringCase("on conflict (consumer_name, message_id) do nothing");
        assertThat(recorded.parameters()).hasSize(6);
        assertThat(recorded.parameters().get(1)).isEqualTo("msg-1");
        assertThat(recorded.parameters().get(2)).isEqualTo("billing");
        assertThat(recorded.parameters().get(3)).isEqualTo("PROCESSING");
    }

    @Test
    void mysqlUsesIgnoreInsertAndReturnsFalseWhenTheRowAlreadyExists() {
        RecordedNativeQuery recorded = new RecordedNativeQuery(0);

        boolean claimed = new MySqlJpaInboxClaimStrategy()
                .tryClaim(recorded.entityManager(), "msg-1", "billing", Instant.parse("2026-07-16T10:15:30Z"));

        assertThat(claimed).isFalse();
        assertThat(recorded.sql()).containsIgnoringCase("insert ignore into jfoundry_inbox_message");
        assertThat(recorded.parameters()).hasSize(6);
        assertThat(recorded.parameters().get(1)).isEqualTo("msg-1");
        assertThat(recorded.parameters().get(2)).isEqualTo("billing");
        assertThat(recorded.parameters().get(3)).isEqualTo("PROCESSING");
    }

    private static final class RecordedNativeQuery {

        private final int updateCount;
        private final List<Object> parameters = new ArrayList<>();
        private String sql;

        private RecordedNativeQuery(int updateCount) {
            this.updateCount = updateCount;
        }

        private EntityManager entityManager() {
            return (EntityManager) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{EntityManager.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("createNativeQuery")) {
                            sql = (String) args[0];
                            return query();
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Query query() {
            return (Query) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Query.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("setParameter")) {
                            parameters.add(args[1]);
                            return proxy;
                        }
                        if (method.getName().equals("executeUpdate")) {
                            return updateCount;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        private String sql() {
            return sql;
        }

        private List<Object> parameters() {
            return parameters;
        }
    }
}
