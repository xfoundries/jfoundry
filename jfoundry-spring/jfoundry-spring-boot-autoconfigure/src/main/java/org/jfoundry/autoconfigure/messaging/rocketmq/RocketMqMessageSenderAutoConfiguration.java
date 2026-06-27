package org.jfoundry.autoconfigure.messaging.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.rocketmq.RocketMqMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureBefore(MessageSenderAutoConfiguration.class)
@ConditionalOnClass(DefaultMQProducer.class)
@EnableConfigurationProperties(RocketMqMessageSenderProperties.class)
@ConditionalOnProperty(prefix = "jfoundry.messaging.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqMessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public RocketMqMessageSender rocketMqMessageSender(
            DefaultMQProducer producer,
            RocketMqMessageSenderProperties properties) {
        return new RocketMqMessageSender(producer, properties.getSendTimeout());
    }
}
