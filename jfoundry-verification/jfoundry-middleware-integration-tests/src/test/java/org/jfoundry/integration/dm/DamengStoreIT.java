package org.jfoundry.integration.dm;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jfoundry.application.inbox.InboxMessageStatus;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.infrastructure.inbox.mybatis.InboxMessageData;
import org.jfoundry.infrastructure.inbox.mybatis.InboxMessageMapper;
import org.jfoundry.infrastructure.inbox.mybatis.MybatisPlusInboxMessageStore;
import org.jfoundry.infrastructure.outbox.mybatis.MybatisPlusOutboxMessageStore;
import org.jfoundry.infrastructure.outbox.mybatis.OutboxData;
import org.jfoundry.infrastructure.outbox.mybatis.OutboxMapper;
import org.jfoundry.integration.support.OutboxMessages;
import org.jfoundry.integration.support.SqlScripts;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(classes = DamengStoreIT.DamengDatabaseConfig.class)
@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = "DM_JDBC_URL", matches = ".+",
                disabledReason = "Dameng integration test skipped: DM_JDBC_URL is required."),
        @EnabledIfEnvironmentVariable(named = "DM_USERNAME", matches = ".+",
                disabledReason = "Dameng integration test skipped: DM_USERNAME is required."),
        @EnabledIfEnvironmentVariable(named = "DM_PASSWORD", matches = ".+",
                disabledReason = "Dameng integration test skipped: DM_PASSWORD is required.")
})
class DamengStoreIT {

    @Autowired
    private MybatisPlusOutboxMessageStore outboxStore;

    @Autowired
    private MybatisPlusInboxMessageStore inboxStore;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private InboxMessageMapper inboxMapper;

    @BeforeAll
    static void createSchema(@Autowired DataSource dataSource) {
        SqlScripts.run(dataSource,
                "db/migration/V20260617__create_outbox_event_dm.sql",
                "db/migration/V20260624__create_inbox_message.sql");
    }

    @BeforeEach
    void cleanDb() {
        outboxMapper.delete(null);
        inboxMapper.delete(null);
    }

    @Test
    void outboxRejectsStaleClaimTokenOnDameng() {
        outboxStore.append(OutboxMessages.pending("evt-1"));
        OutboxMessage firstClaim = outboxStore.claimDispatchable(1, "pod-a").get(0);
        outboxStore.recoverStuckDispatching(Instant.now().plusSeconds(1));
        OutboxMessage secondClaim = outboxStore.claimDispatchable(1, "pod-b").get(0);

        outboxStore.markAsPublished("evt-1", firstClaim.getClaimToken());

        OutboxData data = outboxMapper.selectById("evt-1");
        assertThat(data.getStatus()).isEqualTo("DISPATCHING");
        assertThat(data.getClaimToken()).isEqualTo(secondClaim.getClaimToken());

        outboxStore.markAsPublished("evt-1", secondClaim.getClaimToken());

        OutboxData published = outboxMapper.selectById("evt-1");
        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
        assertThat(published.getClaimToken()).isNull();
    }

    @Test
    void inboxRejectsDuplicateStartOnDameng() {
        assertThat(inboxStore.tryStartProcessing("evt-1", "projection")).isTrue();
        assertThat(inboxStore.tryStartProcessing("evt-1", "projection")).isFalse();

        InboxMessageData record = inboxMapper.selectById(
                inboxMapper.selectList(null).get(0).getId());
        assertThat(record.getStatus()).isEqualTo(InboxMessageStatus.PROCESSING.name());

        inboxStore.markProcessed("evt-1", "projection");

        assertThat(inboxStore.tryStartProcessing("evt-1", "projection")).isFalse();
        assertThat(inboxStore.isProcessed("evt-1", "projection")).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = {OutboxMapper.class, InboxMessageMapper.class})
    static class DamengDatabaseConfig {

        @Bean
        DataSource dataSource() {
            String jdbcUrl = env("DM_JDBC_URL");
            String username = env("DM_USERNAME");
            String password = env("DM_PASSWORD");
            Assumptions.assumeTrue(jdbcUrl != null && username != null && password != null,
                    "Dameng integration test skipped: DM_JDBC_URL, DM_USERNAME, and DM_PASSWORD are required.");
            String driverClass = System.getenv().getOrDefault("DM_DRIVER_CLASS", "dm.jdbc.driver.DmDriver");
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                fail("Dameng JDBC driver class not found: " + driverClass
                        + ". Add the DM JDBC driver to the test runtime classpath.", e);
            }
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverClass);
            dataSource.setUrl(jdbcUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            return dataSource;
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.DM));
            return interceptor;
        }

        @Bean
        MybatisPlusOutboxMessageStore mybatisPlusOutboxMessageStore(OutboxMapper outboxMapper,
                                                                   MybatisPlusInterceptor interceptor) {
            return new MybatisPlusOutboxMessageStore(outboxMapper, interceptor);
        }

        @Bean
        MybatisPlusInboxMessageStore mybatisPlusInboxMessageStore(InboxMessageMapper inboxMessageMapper) {
            return new MybatisPlusInboxMessageStore(inboxMessageMapper);
        }

        private static String env(String name) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? null : value;
        }
    }
}
