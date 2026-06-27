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
import org.springframework.kafka.core.KafkaOperations;

import java.time.Duration;

@AutoConfiguration
@AutoConfigureBefore(MessageSenderAutoConfiguration.class)
@ConditionalOnClass(KafkaOperations.class)
public class KafkaMessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaOperations.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public KafkaMessageSender kafkaMessageSender(KafkaOperations<String, String> kafkaOperations) {
        return new KafkaMessageSender(kafkaOperations, Duration.ofSeconds(10));
    }
}
