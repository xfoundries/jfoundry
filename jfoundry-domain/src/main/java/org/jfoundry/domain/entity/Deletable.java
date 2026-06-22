package org.jfoundry.domain.entity;

import java.time.LocalDateTime;

/// 可删除能力。
/// <p>
/// 该接口只表达领域对象具备软删除状态和删除审计信息，不绑定具体持久化框架或字段注解。
public interface Deletable {

    boolean isDeleted();

    LocalDateTime getDeletedTime();

    String getDeleterId();

    String getDeleterName();
}
