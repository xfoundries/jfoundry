package org.jfoundry.infrastructure.messaging.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jfoundry.application.messaging.PayloadSerializer;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;

/// Outbox payload 默认序列化器（Jackson + JSR-310）。
/// <p>
/// 激活 {@code WRITE_DATES_AS_TIMESTAMPS=false} 以输出 ISO-8601 字符串；
/// 启用 default typing {@code @class} 字段，便于反序列化时还原具体事件类型。
/// <p>
/// 业务侧需要替换序列化实现时，注册自己的 {@link PayloadSerializer} Bean 覆盖默认即可。
@SecondaryAdapter
public class JacksonPayloadSerializer implements PayloadSerializer {

    private final ObjectMapper objectMapper;

    public JacksonPayloadSerializer(ObjectMapper objectMapper) {
        PolymorphicTypeValidator validator = objectMapper.getPolymorphicTypeValidator();
        if (validator == null) {
            validator = LaissezFaireSubTypeValidator.instance;
        }
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        validator,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);
    }

    @Override
    public String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload: " + event.getClass().getName(), e);
        }
    }
}
