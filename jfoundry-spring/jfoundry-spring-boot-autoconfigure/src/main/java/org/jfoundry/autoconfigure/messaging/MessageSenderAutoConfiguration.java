package org.jfoundry.autoconfigure.messaging;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.spring.sender.LoggingMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// MessageSender 默认实现的自动配置。
/// <p>
/// 当业务侧未提供 MessageSender Bean 时，注册 LoggingMessageSender 作为默认实现。
/// LoggingMessageSender 只记录消息并返回失败结果，避免 Outbox 将未外部投递的消息标记为成功。
@AutoConfiguration
public class MessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageSender.class)
    public MessageSender loggingMessageSender() {
        return new LoggingMessageSender();
    }
}
