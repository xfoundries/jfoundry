package org.jfoundry.infrastructure.messaging;

/// MQ 发送抽象。业务侧提供具体实现（Kafka/RabbitMQ 等）。
public interface MessageSender {

    /// @param topic      目标 topic（来自 @MessageRouting 或 @Externalized.value）
    /// @param payloadKey routing key（可能为 null，由具体 MQ 实现决定是否使用）
    /// @param payload    序列化后的 JSON 字符串
    /// @return 发送结果
    SendResult send(String topic, String payloadKey, String payload);
}
