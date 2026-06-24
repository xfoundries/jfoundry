package org.jfoundry.infrastructure.messaging.externalization;

import org.jmolecules.event.annotation.Externalized;
import org.jmolecules.event.types.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// 解析领域事件的外部化规则：
/// <ol>
///   <li>类上有 {@link MessageRouting} → 使用其 {@code topic()}（最高优先级）。</li>
///   <li>类上无 {@code @MessageRouting}，有 {@link Externalized} 且 {@code value()} 非空 → 使用 {@code value()}。</li>
///   <li>类上无 {@code @MessageRouting}，{@code @Externalized.value()} 为空 → fail-fast。</li>
///   <li>类上无 {@code @Externalized} → 返回空 {@code Optional}。</li>
/// </ol>
public class ExternalizationRuleResolver {

    private static final Logger log = LoggerFactory.getLogger(ExternalizationRuleResolver.class);

    private final Map<Class<?>, ResolvedMetadata> cache = new ConcurrentHashMap<>();

    public Optional<ExternalizationRule> resolve(DomainEvent event) {
        Class<?> eventType = event.getClass();
        ResolvedMetadata metadata = cache.computeIfAbsent(eventType, this::computeMetadata);
        if (!metadata.externalized()) {
            if (metadata.routingOnly()) {
                log.warn("类 {} 标记了 @MessageRouting 但未标记 @Externalized，事件将不参与外部化。"
                        + "若需外部化，请同时标注 @Externalized。", eventType.getName());
            }
            return Optional.empty();
        }
        String payloadKey = evaluateKey(event, metadata.keyPath());
        return Optional.of(new ExternalizationRule(metadata.topic(), payloadKey));
    }

    private ResolvedMetadata computeMetadata(Class<?> eventType) {
        MessageRouting routing = eventType.getAnnotation(MessageRouting.class);
        Externalized externalized = eventType.getAnnotation(Externalized.class);
        boolean hasExternalized = externalized != null;
        boolean hasRouting = routing != null;

        if (!hasExternalized) {
            return new ResolvedMetadata(false, hasRouting, null, null);
        }

        String topic;
        if (hasRouting) {
            topic = routing.topic();
        } else {
            String externalizedValue = externalized.value();
            if (externalizedValue == null || externalizedValue.isEmpty()) {
                throw new IllegalStateException(
                        "事件类 " + eventType.getName() + " 标记了 @Externalized 但未指定 topic，"
                                + "请使用 @MessageRouting(topic = ...) 或 @Externalized(\"<topic>\") 显式指定 topic。");
            }
            topic = externalizedValue;
        }

        String keyPath = null;
        if (hasRouting && !routing.key().isEmpty()) {
            keyPath = PropertyPathReader.normalize(routing.key());
        }
        return new ResolvedMetadata(true, false, topic, keyPath);
    }

    private String evaluateKey(DomainEvent event, String keyPath) {
        if (keyPath == null) {
            return null;
        }
        try {
            Object value = PropertyPathReader.read(event, keyPath);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.warn("事件 {} 的 @MessageRouting.key 属性路径解析失败，降级 payloadKey=null。原因：{}",
                    event.getClass().getName(), e.getMessage());
            return null;
        }
    }

    private record ResolvedMetadata(boolean externalized, boolean routingOnly,
                                     String topic, String keyPath) {
    }
}
