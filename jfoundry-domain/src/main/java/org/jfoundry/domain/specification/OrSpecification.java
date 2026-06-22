package org.jfoundry.domain.specification;

import java.util.Objects;

/// 或运算领域规约。
/// <p>
/// 至少一个领域规约满足时返回 true。
///
/// @param left 左侧领域规约
/// @param right 右侧领域规约
/// @param <T> 候选对象类型
public record OrSpecification<T>(
        Specification<T> left,
        Specification<T> right
) implements Specification<T> {

    public OrSpecification {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate);
    }
}
