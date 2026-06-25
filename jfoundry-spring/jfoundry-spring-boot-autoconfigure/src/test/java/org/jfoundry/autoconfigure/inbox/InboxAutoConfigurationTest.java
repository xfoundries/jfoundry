package org.jfoundry.autoconfigure.inbox;

import org.jfoundry.application.inbox.InboxMessageStore;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.infrastructure.inbox.mybatis.InboxMessageMapper;
import org.jfoundry.infrastructure.inbox.mybatis.MybatisPlusInboxMessageStore;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    InboxAutoConfiguration.class,
                    InboxMybatisPlusAutoConfiguration.class));

    @Test
    void createsInboxTemplateWhenMessageStoreExists() {
        runner.withBean(InboxMessageStore.class, StubInboxMessageStore::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(InboxTemplate.class);
                    assertThat(context.getBean(InboxTemplate.class)).isInstanceOf(InboxTemplate.class);
                });
    }

    @Test
    void backsOffWhenUserProvidesInboxTemplate() {
        runner.withBean(InboxMessageStore.class, StubInboxMessageStore::new)
                .withBean(InboxTemplate.class, () -> new InboxTemplate(new StubInboxMessageStore()))
                .run(context -> assertThat(context).hasSingleBean(InboxTemplate.class));
    }

    @Test
    void createsMybatisPlusInboxMessageStoreWhenMapperExists() {
        runner.withBean(InboxMessageMapper.class, () -> mock(InboxMessageMapper.class))
                .withBean(SqlSessionFactory.class, () -> mock(SqlSessionFactory.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(InboxMessageStore.class);
                    assertThat(context.getBean(InboxMessageStore.class))
                            .isInstanceOf(MybatisPlusInboxMessageStore.class);
                });
    }

    static class StubInboxMessageStore implements InboxMessageStore {

        @Override
        public boolean isProcessed(String messageId, String consumerName) {
            return false;
        }

        @Override
        public void markProcessed(String messageId, String consumerName) {
        }
    }
}
