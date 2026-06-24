package org.jfoundry.infrastructure.persistence;

import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.Objects;

/// 聚合根持久化数据对象基类。
/// <p>
/// 该类型用于领域聚合仓储链路，保留 jMolecules Identifier 约束，确保领域聚合 ID
/// 与持久化数据对象 ID 使用同一个强类型标识符。技术表、读模型或框架内部数据表应定义自己的
/// 数据对象，不需要伪装成领域 Identifier。
///
/// @param <ID> 聚合标识符类型，必须是 jMolecules Identifier 且可序列化
public abstract class AggregateData<ID extends Identifier & Serializable> {

    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    /// hashCode 与 equals 基于 ID 判断。
    /// <p>
    /// ID 为 null 时（未持久化的新建对象），hashCode 回退到对象身份哈希，
    /// equals 拒绝返回 true，避免两个不同的新建对象被 HashSet/HashMap 折叠。
    /// 这是 JPA/Hibernate {@code AbstractPersistable} 的标准做法。
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AggregateData<?> that = (AggregateData<?>) obj;
        if (id == null || that.id == null) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                '}';
    }
}
