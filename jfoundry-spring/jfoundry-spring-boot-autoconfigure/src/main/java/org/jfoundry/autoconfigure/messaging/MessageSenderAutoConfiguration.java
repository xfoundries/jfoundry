package org.jfoundry.autoconfigure.messaging;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.spring.sender.LoggingMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// Auto-configuration for the default MessageSender implementation.
/// <p>
/// Registers LoggingMessageSender as the default implementation when the application has not
/// provided a MessageSender bean. LoggingMessageSender only logs messages and returns failures, so
/// Outbox does not mark externally undelivered messages as successful.
@AutoConfiguration
@ConditionalOnClass({MessageSender.class, LoggingMessageSender.class})
public class MessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageSender.class)
    public MessageSender loggingMessageSender() {
        return new LoggingMessageSender();
    }
}
