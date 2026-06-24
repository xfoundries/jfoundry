package org.jfoundry.application.messaging.externalization;

/// 外部化规则解析结果。
/// @param topic      目标 topic
/// @param payloadKey routing key（可能为 null）
public record ExternalizationRule(String topic, String payloadKey) {
}
