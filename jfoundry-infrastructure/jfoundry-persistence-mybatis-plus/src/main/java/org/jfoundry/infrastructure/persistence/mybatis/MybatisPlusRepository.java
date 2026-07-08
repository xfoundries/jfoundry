package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceRepository;
import org.jfoundry.infrastructure.persistence.AggregateData;
import org.jfoundry.infrastructure.persistence.DataConverter;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;

/// MyBatis-Plus repository implementation based on the AbstractPersistenceRepository template base class.
/// <p>
/// This class implements only the four template methods: insertData, updateData, deleteDataById,
/// and selectDataById. Aggregate lifecycle handling, event handoff, and modify/remove zero-row
/// defenses are provided by the parent class.
/// <p>
/// Conditional queries, counts, paging, and maintenance deletes should be expressed by concrete
/// business boundaries and implemented in subclasses with native MyBatis capabilities.
///
/// @param <T>  aggregate root type, which must be both a jMolecules AggregateRoot and a framework EventRecordable
/// @param <ID> domain identifier type
/// @param <D>  database entity type
/// @param <K>  persistence primary-key type
public abstract class MybatisPlusRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends AggregateData<K>,
        K extends Serializable>
        extends AbstractPersistenceRepository<T, ID, D, K> {

    protected final BaseMapper<D> mapper;

    protected MybatisPlusRepository(BaseMapper<D> mapper,
                                    DataConverter<T, ID, D, K> converter) {
        super(converter);
        this.mapper = mapper;
    }

    @Override
    protected void insertData(D data) {
        mapper.insert(data);
    }

    @Override
    protected long updateData(D data) {
        return mapper.updateById(data);
    }

    @Override
    protected long deleteDataById(K id) {
        return mapper.deleteById(id);
    }

    @Override
    protected D selectDataById(K id) {
        return mapper.selectById(id);
    }
}
