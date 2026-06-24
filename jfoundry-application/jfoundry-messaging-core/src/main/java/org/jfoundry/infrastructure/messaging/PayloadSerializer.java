package org.jfoundry.infrastructure.messaging;

/// Outbox payload 序列化抽象。
/// <p>
/// 业务侧可以提供自己的实现（例如 Jackson / Gson / Protobuf）。框架提供的 Jackson
/// 默认实现位于 {@code jfoundry-messaging-jackson}，core 模块只保留 SPI。
public interface PayloadSerializer {

    /// @param event 已经发生的领域事件（通常带 {@code @Externalized} 标记）
    /// @return 序列化后的字符串（默认实现为 JSON），将写入 Outbox 的 {@code payload_json} 列
    String serialize(Object event);
}
