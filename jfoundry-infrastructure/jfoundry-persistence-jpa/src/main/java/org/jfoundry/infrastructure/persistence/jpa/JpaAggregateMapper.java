package org.jfoundry.infrastructure.persistence.jpa;

/// Maps between a pure domain aggregate and one Jakarta Persistence entity graph.
///
/// @param <A> aggregate type
/// @param <ID> aggregate identifier type
/// @param <E> JPA entity type
/// @param <K> JPA identifier type
public interface JpaAggregateMapper<A, ID, E, K> {

    K toEntityId(ID id);

    E newEntity(A aggregate);

    A toAggregate(E entity);

    /// Applies current aggregate state to the managed entity loaded in this persistence context.
    void apply(A aggregate, E managedEntity);
}
