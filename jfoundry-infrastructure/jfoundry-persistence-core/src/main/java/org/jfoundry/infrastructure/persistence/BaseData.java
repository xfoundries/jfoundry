package org.jfoundry.infrastructure.persistence;

import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.Objects;

/// 持久化数据对象基类。
/// <p>
/// 所有 MyBatis/JPA 等持久化数据类的基类，提供统一的标识符字段。具体的 ORM 主键策略由子类按需标注
/// （例如 MyBatis-Plus 的 {@code @TableId}、JPA 的 {@code @Id}），基类不再代标注，避免 SPI 被特定 ORM 绑定。
///
/// @param <ID> 标识符类型，必须是 jMolecules Identifier 且可序列化（满足 MyBatis-Plus/JPA 主键约束）
public abstract class BaseData<ID extends Identifier & Serializable> {

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
        BaseData<?> that = (BaseData<?>) obj;
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
