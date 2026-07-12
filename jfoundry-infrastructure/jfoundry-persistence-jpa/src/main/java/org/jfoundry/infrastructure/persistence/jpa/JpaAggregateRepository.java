package org.jfoundry.infrastructure.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.infrastructure.persistence.AbstractAggregateRepository;
import org.jfoundry.infrastructure.persistence.AggregatePersistenceContext;
import org.jfoundry.infrastructure.persistence.PersistenceStateKey;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.util.Objects;

/// Jakarta Persistence repository base for aggregates represented by one entity graph.
/// <p>
/// Loaded managed entities are tracked outside the domain aggregate. Modification applies state
/// to that same entity and never calls {@link EntityManager#merge(Object)}. The application must
/// keep load and modification in one transaction and persistence context.
public abstract class JpaAggregateRepository<
        A extends AggregateRoot<A, ID> & EventRecordable,
        ID extends Identifier,
        E,
        K>
        extends AbstractAggregateRepository<A, ID> {

    protected final EntityManager entityManager;
    private final Class<E> entityType;
    private final JpaAggregateMapper<A, ID, E, K> mapper;
    private final AggregatePersistenceContext persistenceContext;
    private final PersistenceStateKey<E> managedEntityKey;

    protected JpaAggregateRepository(
            EntityManager entityManager,
            Class<E> entityType,
            JpaAggregateMapper<A, ID, E, K> mapper,
            AggregatePersistenceContext persistenceContext) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager must not be null.");
        this.entityType = Objects.requireNonNull(entityType, "Entity type must not be null.");
        this.mapper = Objects.requireNonNull(mapper, "JpaAggregateMapper must not be null.");
        this.persistenceContext = Objects.requireNonNull(
                persistenceContext, "AggregatePersistenceContext must not be null.");
        this.managedEntityKey = PersistenceStateKey.of(
                entityType.getName() + ".managed-entity", entityType);
    }

    protected final JpaAggregateMapper<A, ID, E, K> mapper() {
        return mapper;
    }

    protected final AggregatePersistenceContext persistenceContext() {
        return persistenceContext;
    }

    @Override
    protected final A doFindById(ID id) {
        E entity = entityManager.find(entityType, mapper.toEntityId(id));
        if (entity == null) {
            return null;
        }
        A aggregate = mapper.toAggregate(entity);
        persistenceContext.attach(aggregate, managedEntityKey, entity);
        return aggregate;
    }

    @Override
    protected final void doAdd(A aggregate) {
        E entity = mapper.newEntity(aggregate);
        entityManager.persist(entity);
        flush("add", aggregate);
        persistenceContext.attach(aggregate, managedEntityKey, entity);
    }

    @Override
    protected final void doModify(A aggregate) {
        E managedEntity = persistenceContext.require(aggregate, managedEntityKey);
        mapper.apply(aggregate, managedEntity);
        flush("modify", aggregate);
    }

    @Override
    protected final void doRemove(A aggregate) {
        E managedEntity = persistenceContext.require(aggregate, managedEntityKey);
        entityManager.remove(managedEntity);
        flush("remove", aggregate);
    }

    private void flush(String operation, A aggregate) {
        try {
            entityManager.flush();
        } catch (OptimisticLockException failure) {
            throw new ConflictException(
                    operation + " optimistic lock conflict for aggregate: " + aggregate.getId(),
                    failure);
        }
    }
}
