package org.jfoundry.infrastructure.messaging.spring.sender;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// MessageSender fallback 实现：仅打日志，不真正发送。
/// <p>
/// 业务方在 starter 中通过 {@code @ConditionalOnMissingBean(MessageSender.class)} 兜底注册；
/// 业务方覆盖此 Bean 即接入真实 MQ。
public class LoggingMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessageSender.class);

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        log.info("[LoggingMessageSender] topic={}, key={}, payload={}", topic, payloadKey, payload);
        return SendResult.ok();
    }
}
