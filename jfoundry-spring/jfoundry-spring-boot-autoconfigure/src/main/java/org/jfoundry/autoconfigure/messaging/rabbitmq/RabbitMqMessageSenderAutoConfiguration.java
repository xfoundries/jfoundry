package org.jfoundry.autoconfigure.messaging.rabbitmq;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.rabbitmq.RabbitMqMessageSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureBefore(MessageSenderAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMqMessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public RabbitMqMessageSender rabbitMqMessageSender(RabbitTemplate rabbitTemplate) {
        return new RabbitMqMessageSender(rabbitTemplate);
    }
}
