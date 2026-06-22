package org.jfoundry.domain.entity;

import java.time.LocalDateTime;

/// 可审计能力。
/// <p>
/// 该接口只表达领域对象具备创建和修改审计信息，不强制对象继承固定字段基类。
/// 使用非标准字段、外部审计组件或自定义持久化模型时，可以直接实现该接口。
public interface Auditable {

    String getCreatorId();

    String getCreatorName();

    LocalDateTime getCreatedTime();

    String getLastModifierId();

    String getLastModifierName();

    LocalDateTime getLastModifiedTime();
}
