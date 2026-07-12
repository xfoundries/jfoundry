package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.infrastructure.persistence.AbstractAggregateRepository;
import org.jfoundry.infrastructure.persistence.AggregateData;
import org.jfoundry.infrastructure.persistence.DataConverter;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.Objects;

/// MyBatis-Plus convenience base for aggregates represented by one persistence data object.
/// <p>
/// This specialization converts the aggregate to one {@link AggregateData} object and delegates
/// storage to one {@link BaseMapper}. Business adapters that coordinate multiple tables, mappers,
/// or stores should extend {@link AbstractAggregateRepository} directly instead.
///
/// @param <T> aggregate root type
/// @param <ID> aggregate identifier type
/// @param <D> persistence data type
/// @param <K> persistence identifier type
public abstract class MybatisPlusAggregateRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends AggregateData<K>,
        K extends Serializable>
        extends AbstractAggregateRepository<T, ID> {

    protected final BaseMapper<D> mapper;
    private final DataConverter<T, ID, D, K> converter;

    protected MybatisPlusAggregateRepository(
            BaseMapper<D> mapper,
            DataConverter<T, ID, D, K> converter) {
        this.mapper = Objects.requireNonNull(mapper, "Mapper must not be null.");
        this.converter = Objects.requireNonNull(converter, "DataConverter must not be null.");
    }

    /// Converter available to subclasses for business-specific queries.
    protected final DataConverter<T, ID, D, K> converter() {
        return converter;
    }

    @Override
    protected final T doFindById(ID id) {
        D data = mapper.selectById(converter.toDataId(id));
        return data == null ? null : converter.toEntity(data);
    }

    @Override
    protected final void doAdd(T aggregate) {
        mapper.insert(converter.toData(aggregate));
    }

    @Override
    protected final void doModify(T aggregate) {
        long count = mapper.updateById(converter.toData(aggregate));
        requireAffectedRow(
                count,
                "modify affected 0 rows - aggregate not found or optimistic lock conflict: "
                        + aggregate.getId());
    }

    @Override
    protected final void doRemove(T aggregate) {
        long count = mapper.deleteById(converter.toDataId(aggregate.getId()));
        requireAffectedRow(count, "remove affected 0 rows - aggregate not found: " + aggregate.getId());
    }

    private static void requireAffectedRow(long count, String message) {
        if (count == 0) {
            throw new IllegalStateException(message);
        }
    }
}
