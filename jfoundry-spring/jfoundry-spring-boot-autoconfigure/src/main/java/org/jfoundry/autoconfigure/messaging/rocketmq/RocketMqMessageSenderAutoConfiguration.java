package org.jfoundry.autoconfigure.messaging.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.rocketmq.RocketMqMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@AutoConfiguration
@AutoConfigureBefore(MessageSenderAutoConfiguration.class)
@ConditionalOnClass(DefaultMQProducer.class)
public class RocketMqMessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnBean(MQProducer.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public RocketMqMessageSender rocketMqMessageSender(MQProducer producer) {
        return new RocketMqMessageSender(producer, Duration.ofSeconds(10));
    }
}
