package org.jfoundry.infrastructure.persistence;

import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/// Storage-neutral lifecycle base for aggregate repository adapters.
/// <p>
/// Subclasses implement complete aggregate persistence operations. A complete operation may use
/// one data object, multiple tables, multiple mappers, or another storage model. Public lifecycle
/// methods provide the supported lifecycle entry points, while protected {@code do*} methods are
/// the persistence-specific extension points. Lifecycle methods remain non-final so runtime
/// frameworks can create class-based proxies for transactions and other cross-cutting concerns.
///
/// @param <T> aggregate root type
/// @param <ID> aggregate identifier type
public abstract class AbstractAggregateRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier>
        implements AggregateRepository<T, ID> {

    private DomainEventContext domainEventContext;
    private PersistenceFailureTranslator persistenceFailureTranslator =
            PersistenceFailureTranslator.passThrough();

    /// Injects the event context used to register successfully persisted aggregates.
    public final void setDomainEventContext(DomainEventContext domainEventContext) {
        this.domainEventContext = Objects.requireNonNull(
                domainEventContext, "DomainEventContext must not be null.");
    }

    /// Injects an optional runtime-specific persistence failure translator.
    public final void setPersistenceFailureTranslator(
            PersistenceFailureTranslator persistenceFailureTranslator) {
        this.persistenceFailureTranslator = Objects.requireNonNull(
                persistenceFailureTranslator, "PersistenceFailureTranslator must not be null.");
    }

    /// Loads and restores one complete aggregate, returning null when it does not exist.
    protected abstract T doFindById(ID id);

    /// Persists one complete new aggregate.
    protected abstract void doAdd(T aggregate);

    /// Persists all changes to one complete existing aggregate.
    protected abstract void doModify(T aggregate);

    /// Removes one complete aggregate according to the adapter's storage semantics.
    protected abstract void doRemove(T aggregate);

    @Override
    public T findById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("Aggregate id must not be null.");
        }
        return execute(PersistenceOperation.FIND, () -> doFindById(id));
    }

    @Override
    public void add(T aggregate) {
        T validatedAggregate = requireAggregate(aggregate);
        execute(PersistenceOperation.ADD, () -> doAdd(validatedAggregate));
        registerAggregate(validatedAggregate);
    }

    @Override
    public void modify(T aggregate) {
        T validatedAggregate = requireAggregate(aggregate);
        execute(PersistenceOperation.MODIFY, () -> doModify(validatedAggregate));
        registerAggregate(validatedAggregate);
    }

    @Override
    public void addAll(Collection<T> aggregates) {
        List<T> aggregateList = requireAggregates(aggregates);
        aggregateList.forEach(aggregate ->
                execute(PersistenceOperation.ADD, () -> doAdd(aggregate)));
        aggregateList.forEach(this::registerAggregate);
    }

    @Override
    public void modifyAll(Collection<T> aggregates) {
        List<T> aggregateList = requireAggregates(aggregates);
        aggregateList.forEach(aggregate ->
                execute(PersistenceOperation.MODIFY, () -> doModify(aggregate)));
        aggregateList.forEach(this::registerAggregate);
    }

    @Override
    public void remove(T aggregate) {
        T validatedAggregate = requireAggregate(aggregate);
        if (validatedAggregate.getId() == null) {
            throw new IllegalArgumentException("Aggregate id must not be null.");
        }
        execute(PersistenceOperation.REMOVE, () -> doRemove(validatedAggregate));
        registerAggregate(validatedAggregate);
    }

    private T requireAggregate(T aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("Aggregate must not be null.");
        }
        return aggregate;
    }

    private List<T> requireAggregates(Collection<T> aggregates) {
        if (aggregates == null) {
            throw new IllegalArgumentException("Aggregates must not be null.");
        }
        if (aggregates.isEmpty()) {
            return List.of();
        }
        for (T aggregate : aggregates) {
            requireAggregate(aggregate);
        }
        return List.copyOf(aggregates);
    }

    private void registerAggregate(T aggregate) {
        if (domainEventContext != null) {
            domainEventContext.register(aggregate);
        }
    }

    private void execute(PersistenceOperation operation, Runnable persistenceCall) {
        execute(operation, () -> {
            persistenceCall.run();
            return null;
        });
    }

    private <R> R execute(PersistenceOperation operation, Supplier<R> persistenceCall) {
        try {
            return persistenceCall.get();
        } catch (RuntimeException failure) {
            RuntimeException translated = persistenceFailureTranslator.translate(operation, failure);
            if (translated == null) {
                throw new IllegalStateException(
                        "PersistenceFailureTranslator must not return null.", failure);
            }
            throw translated;
        }
    }
}
