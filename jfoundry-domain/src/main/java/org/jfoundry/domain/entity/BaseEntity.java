package org.jfoundry.domain.entity;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;

/// 聚合内实体基类。
///
/// 提供聚合内实体的通用实现：
/// - 标识符管理。
/// - 不提供事件发布能力，领域事件由聚合根统一记录。
/// - 不持有 parent 引用，避免实体到聚合根的隐式事件冒泡。
///
/// @param <T>  所属聚合根类型（self type，与 BaseAggregateRoot 的 T 一致）
/// @param <ID> 标识符类型
///
public abstract class BaseEntity<T extends AggregateRoot<T, ID>, ID extends Identifier>
        implements Entity<T, ID> {

    /// 标识符。
    private ID id;

    public BaseEntity(ID id) {
        this.id = id;
    }

    @Override
    public ID getId() {
        return id;
    }

    /// 重新指定标识符。
    /// <p>
    /// 该方法只面向子类和持久化转换场景开放，业务代码应优先通过构造函数确定实体标识。
    ///
    /// @param id 标识符
    protected void identify(ID id) {
        this.id = id;
    }
}
