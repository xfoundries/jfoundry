package org.jfoundry.autoconfigure.inbox;

import org.jfoundry.application.inbox.InboxMessageStore;
import org.jfoundry.application.inbox.InboxTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(InboxTemplate.class)
public class InboxAutoConfiguration {

    @Bean
    @ConditionalOnBean(InboxMessageStore.class)
    @ConditionalOnMissingBean(InboxTemplate.class)
    public InboxTemplate inboxTemplate(InboxMessageStore store) {
        return new InboxTemplate(store);
    }
}
