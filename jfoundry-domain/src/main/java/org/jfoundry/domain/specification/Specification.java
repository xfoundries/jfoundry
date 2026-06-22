package org.jfoundry.domain.specification;

/// 领域规约。
/// <p>
/// 用于判断一个已经加载到内存中的候选对象是否满足业务规则。
/// 规约不承诺可以转换为数据库条件；持久化适配器应使用底层 ORM 或 SQL 的原生表达能力。
///
/// @param <T> 候选对象类型
public interface Specification<T> {

    /// 判断候选对象是否满足规约。
    ///
    /// @param candidate 待判断的候选对象
    /// @return true 满足规约，false 不满足
    boolean isSatisfiedBy(T candidate);

    /// 与运算 - 两个规约都满足。
    ///
    /// @param other 另一个领域规约
    /// @return 组合后的领域规约
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }

    /// 或运算 - 至少一个规约满足。
    ///
    /// @param other 另一个领域规约
    /// @return 组合后的领域规约
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /// 非运算 - 规约取反。
    ///
    /// @return 取反后的领域规约
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}
