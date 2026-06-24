package org.jfoundry.autoconfigure.messaging.kafka;

import org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration;
import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.messaging.kafka.KafkaMessageSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@AutoConfigureBefore(MessageSenderAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaMessageSenderProperties.class)
@ConditionalOnProperty(prefix = "jfoundry.messaging.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaMessageSenderAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public KafkaMessageSender kafkaMessageSender(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaMessageSenderProperties properties) {
        return new KafkaMessageSender(kafkaTemplate, properties.getSendTimeout());
    }
}
