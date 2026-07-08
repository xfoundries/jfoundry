package org.jfoundry.infrastructure.persistence;

import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/// Abstract base class for persistence repositories using the template method pattern.
/// <p>
/// Skeleton behavior:
/// - context registration: registers aggregates with DomainEventContext after add/modify/addAll/
///   modifyAll/remove succeeds.
/// - defensive contract: throws IllegalStateException when modify/remove affects 0 rows, avoiding
///   silent failures.
/// - batch handling: addAll/modifyAll register each aggregate with the context.
/// <p>
/// Template methods implemented by subclasses:
/// - {@link #insertData}: insert one data object.
/// - {@link #updateData}: update one data object and return affected row count.
/// - {@link #deleteDataById}: delete by ID and return affected row count.
/// - {@link #selectDataById}: select by ID.
/// <p>
/// Example implementation: {@code MybatisPlusRepository} in the {@code jfoundry-persistence-mybatis-plus}
/// module, based on MyBatis-Plus {@code BaseMapper}. Sibling modules such as JPA or Mongo adapters can
/// be added later.
///
/// @param <T>  aggregate root type, which must be both a jMolecules AggregateRoot and a framework EventRecordable
/// @param <ID> domain identifier type
/// @param <D>  database entity type
/// @param <K>  persistence primary-key type
public abstract class AbstractPersistenceRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends AggregateData<K>,
        K extends Serializable>
        implements AggregateRepository<T, ID> {

    private DomainEventContext domainEventContext;
    private final DataConverter<T, ID, D, K> converter;

    protected AbstractPersistenceRepository(DataConverter<T, ID, D, K> converter) {
        this.converter = Objects.requireNonNull(converter, "DataConverter must not be null.");
    }

    /**
     * Injects the event context used to register successfully persisted aggregates.
     * Framework integrations call this after repository construction so business
     * repositories do not have to expose infrastructure constructor parameters.
     */
    public final void setDomainEventContext(DomainEventContext domainEventContext) {
        this.domainEventContext = Objects.requireNonNull(domainEventContext, "DomainEventContext must not be null.");
    }

    /// Template method implemented by subclasses: insert one data object.
    protected abstract void insertData(D data);

    /// Template method implemented by subclasses: update one data object and return affected row count.
    protected abstract long updateData(D data);

    /// Template method implemented by subclasses: delete by ID and return affected row count.
    protected abstract long deleteDataById(K id);

    /// Template method implemented by subclasses: select by ID, returning null when not found.
    protected abstract D selectDataById(K id);

    /// Converter available to subclasses, for example when implementing findById variants.
    protected DataConverter<T, ID, D, K> converter() {
        return converter;
    }

    @Override
    public T findById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity id must not be null.");
        }
        D data = selectDataById(converter.toDataId(id));
        return data == null ? null : converter.toEntity(data);
    }

    @Override
    public void add(T entity) {
        T validatedEntity = requireEntity(entity);
        insertData(converter.toData(validatedEntity));
        registerAggregate(validatedEntity);
    }

    @Override
    public void modify(T entity) {
        T validatedEntity = requireEntity(entity);
        long count = updateData(converter.toData(validatedEntity));
        assertAffectedRows(count,
                "modify affected 0 rows — entity not found or optimistic lock conflict: " + validatedEntity.getId());
        registerAggregate(validatedEntity);
    }

    @Override
    public void addAll(Collection<T> entities) {
        List<T> entityList = requireEntities(entities);
        if (entityList.isEmpty()) {
            return;
        }
        for (T entity : entityList) {
            insertData(converter.toData(entity));
        }
        entityList.forEach(this::registerAggregate);
    }

    @Override
    public void modifyAll(Collection<T> entities) {
        List<T> entityList = requireEntities(entities);
        if (entityList.isEmpty()) {
            return;
        }
        for (T entity : entityList) {
            long count = updateData(converter.toData(entity));
            assertAffectedRows(count,
                    "modifyAll affected 0 rows — entity not found or optimistic lock conflict: " + entity.getId());
        }
        entityList.forEach(this::registerAggregate);
    }

    @Override
    public void remove(T entity) {
        T validatedEntity = requireEntity(entity);
        ID entityId = requireEntityId(validatedEntity);
        long count = deleteDataById(converter.toDataId(entityId));
        assertAffectedRows(count, "remove affected 0 rows — entity not found: " + entityId);
        registerAggregate(validatedEntity);
    }

    private T requireEntity(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        return entity;
    }

    private ID requireEntityId(T entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Entity id must not be null.");
        }
        return entity.getId();
    }

    private List<T> requireEntities(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities must not be null.");
        }
        if (entities.isEmpty()) {
            return List.of();
        }
        for (T entity : entities) {
            if (entity == null) {
                throw new IllegalArgumentException("Entities must not contain null.");
            }
        }
        return List.copyOf(entities);
    }

    private void assertAffectedRows(long count, String message) {
        if (count == 0) {
            throw new IllegalStateException(message);
        }
    }

    /// The persistence layer registers only successfully persisted aggregates; the application
    /// layer extracts and dispatches events at the use-case boundary.
    private void registerAggregate(T entity) {
        if (domainEventContext == null) {
            return;
        }
        domainEventContext.register(entity);
    }
}
