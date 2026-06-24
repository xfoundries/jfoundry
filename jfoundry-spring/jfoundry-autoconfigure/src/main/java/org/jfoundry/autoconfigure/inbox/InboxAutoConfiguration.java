package org.jfoundry.autoconfigure.inbox;

import org.jfoundry.application.inbox.InboxRepository;
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
    @ConditionalOnBean(InboxRepository.class)
    @ConditionalOnMissingBean(InboxTemplate.class)
    public InboxTemplate inboxTemplate(InboxRepository repository) {
        return new InboxTemplate(repository);
    }
}
