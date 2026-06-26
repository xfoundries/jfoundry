package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-3 regression test: verifies that {@code jfoundry.persistence.db-type} flows through
/// {@link OutboxMybatisPlusAutoConfiguration#paginationInnerInterceptor(JfoundryPersistenceProperties)}
/// into the {@link PaginationInnerInterceptor} registered inside the auto-configured
/// {@link MybatisPlusInterceptor} bean.
/// <p>
/// The wiring is a one-line ternary in {@code paginationInnerInterceptor}, but the reviewer
/// flagged that without a runtime assertion on the interceptor's {@code dbType} field, a
/// future refactor (e.g. swapping the {@code new DbTypeResolver().resolveExplicit(...)}
/// call, changing the ternary, or switching constructors) could silently break the
/// "explicit config wins" contract without failing any test.
/// <p>
/// {@link PaginationInnerInterceptor#getDbType()} is a public getter in MP 3.5.12 (the
/// field itself is private), so no reflection is needed. If the getter is removed in a
/// future MP version, fall back to reflecting on {@code dbType}; if that also fails, drop
/// to the weaker assertion that the interceptor bean exists.

/// Explicit {@code jfoundry.persistence.db-type=DM} must reach the
/// {@link PaginationInnerInterceptor} at runtime.
@SpringBootTest(classes = PaginationInterceptorDbTypeWiringTest.ExplicitDbTypeTestApp.class)
@TestPropertySource(properties = {
        "jfoundry.persistence.db-type=DM",
        // Disable the outbox dispatcher so this test exercises only the MybatisPlusInterceptor
        // wiring; otherwise the full autoconfig chain starts a ScheduledOutboxDispatcher whose
        // polling may interact with subsequent tests sharing the same H2 instance.
        "jfoundry.outbox.dispatcher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:jfoundry-db-type-dm;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:outbox_event.sql"
})
class PaginationInterceptorDbTypeWiringTest {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Test
    void paginationInnerInterceptorCarriesExplicitDbType() {
        PaginationInnerInterceptor pagination = findPaginationInnerInterceptor(mybatisPlusInterceptor);

        assertThat(pagination.getDbType())
                .as("jfoundry.persistence.db-type=DM must reach PaginationInnerInterceptor.dbType")
                .isEqualTo(DbType.DM);
    }

    static PaginationInnerInterceptor findPaginationInnerInterceptor(MybatisPlusInterceptor interceptor) {
        List<InnerInterceptor> inners = interceptor.getInterceptors();
        return inners.stream()
                .filter(PaginationInnerInterceptor.class::isInstance)
                .map(PaginationInnerInterceptor.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "PaginationInnerInterceptor not registered in MybatisPlusInterceptor; got: " + inners));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.outbox.mybatis")
    static class ExplicitDbTypeTestApp {
        /// DomainEventOutboxRecorderAutoConfiguration's payloadSerializer bean
        /// pulls in Jackson. Same pattern as OutboxTableNameOverrideTest.
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
