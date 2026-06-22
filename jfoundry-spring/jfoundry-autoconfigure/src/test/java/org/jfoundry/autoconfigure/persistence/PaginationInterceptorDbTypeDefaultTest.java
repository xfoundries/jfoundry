package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
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

import static org.assertj.core.api.Assertions.assertThat;

/// P2-3 regression test: when {@code jfoundry.persistence.db-type} is NOT set,
/// {@link OutboxMybatisPlusAutoConfiguration} must fall back to MP's default no-arg
/// {@link PaginationInnerInterceptor} whose {@code dbType} is {@code null}, so MP
/// auto-detects lazily on the first paging query (preserving backwards compatibility).
@SpringBootTest(classes = PaginationInterceptorDbTypeDefaultTest.NoDbTypeTestApp.class)
@TestPropertySource(properties = {
        "jfoundry.outbox.dispatcher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:jfoundry-db-type-default;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:outbox_event.sql"
})
class PaginationInterceptorDbTypeDefaultTest {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Test
    void paginationInnerInterceptorFallsBackToMpDefault() {
        PaginationInnerInterceptor pagination =
                PaginationInterceptorDbTypeWiringTest.findPaginationInnerInterceptor(mybatisPlusInterceptor);

        assertThat(pagination.getDbType())
                .as("without jfoundry.persistence.db-type, PaginationInnerInterceptor.dbType must stay null so MP auto-detects")
                .isNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.messaging.mybatis.outbox")
    static class NoDbTypeTestApp {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
