package org.jfoundry.domain.specification;

import java.util.Objects;

/// 非运算领域规约。
/// <p>
/// 领域规约取反。
///
/// @param specification 被取反的领域规约
/// @param <T> 候选对象类型
public record NotSpecification<T>(Specification<T> specification) implements Specification<T> {

    public NotSpecification {
        Objects.requireNonNull(specification, "specification must not be null");
    }

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !specification.isSatisfiedBy(candidate);
    }
}
