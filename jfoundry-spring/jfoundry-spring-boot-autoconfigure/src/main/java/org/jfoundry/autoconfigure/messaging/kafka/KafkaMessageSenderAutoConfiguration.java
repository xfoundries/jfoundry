package org.jfoundry.autoconfigure.messaging.kafka;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.kafka.KafkaMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

@AutoConfiguration
@AutoConfigureBefore(MessageSenderAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaMessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public KafkaMessageSender kafkaMessageSender(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaMessageSender(kafkaTemplate, Duration.ofSeconds(10));
    }
}
