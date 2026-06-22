package org.jfoundry.domain.specification;

/// 恒满足领域规约。
///
/// @param <T> 候选对象类型
public final class AlwaysSpecification<T> implements Specification<T> {

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return true;
    }
}
