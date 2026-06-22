package org.jfoundry.infrastructure.messaging;

/// Outbox payload 序列化抽象。
/// <p>
/// 业务侧可以提供自己的实现替换默认 {@code JacksonPayloadSerializer}（例如改用 Gson / Protobuf）。
/// 默认实现位于本模块，构造时接受 {@code com.fasterxml.jackson.databind.ObjectMapper}，
/// 输出 ISO-8601 时间字符串并启用 default typing 便于反序列化时还原具体事件类型。
public interface PayloadSerializer {

    /// @param event 已经发生的领域事件（通常带 {@code @Externalized} 标记）
    /// @return 序列化后的字符串（默认实现为 JSON），将写入 Outbox 的 {@code payload_json} 列
    String serialize(Object event);
}
