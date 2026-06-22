package org.jfoundry.domain.specification;

/// 永不满足领域规约。
///
/// @param <T> 候选对象类型
public final class NeverSpecification<T> implements Specification<T> {

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return false;
    }
}
