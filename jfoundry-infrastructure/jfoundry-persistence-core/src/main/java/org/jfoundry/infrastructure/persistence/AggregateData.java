package org.jfoundry.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/// 聚合根持久化数据对象基类。
/// <p>
/// 该类型用于领域聚合仓储链路中的持久化映射对象。ID 泛型代表持久化层主键类型，
/// 通常是数据库和持久化框架天然支持的 {@link String}、{@link Long}、{@link java.util.UUID} 等类型。
/// 领域聚合根可以继续使用 jMolecules Identifier 强类型 ID，由 DataConverter 在仓储边界完成转换。
///
/// @param <ID> 持久化标识符类型，必须可序列化
public abstract class AggregateData<ID extends Serializable> {

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
