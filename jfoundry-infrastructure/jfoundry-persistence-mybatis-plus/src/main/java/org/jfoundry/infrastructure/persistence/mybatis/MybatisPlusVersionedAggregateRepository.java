package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.infrastructure.persistence.AbstractAggregateRepository;
import org.jfoundry.infrastructure.persistence.AggregateData;
import org.jfoundry.infrastructure.persistence.AggregatePersistenceContext;
import org.jfoundry.infrastructure.persistence.DataConverter;
import org.jfoundry.infrastructure.persistence.PersistenceStateKey;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.Objects;

/// MyBatis-Plus convenience base for one-Data aggregates whose version is persistence-owned.
/// <p>
/// The persistence data type must declare one MyBatis-Plus {@code @Version} field, and the runtime
/// must configure {@code OptimisticLockerInnerInterceptor}. The aggregate itself does not expose
/// the version. Composite adapters may use {@link AggregatePersistenceContext} and
/// {@link VersionedDataAccessor} directly instead of extending this class.
public abstract class MybatisPlusVersionedAggregateRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends AggregateData<K>,
        K extends Serializable,
        V>
        extends AbstractAggregateRepository<T, ID> {

    protected final BaseMapper<D> mapper;
    private final DataConverter<T, ID, D, K> converter;
    private final Class<D> dataType;
    private final VersionedDataAccessor<D, V> versionAccessor;
    private final AggregatePersistenceContext persistenceContext;
    private final PersistenceStateKey<V> versionKey;

    protected MybatisPlusVersionedAggregateRepository(
            BaseMapper<D> mapper,
            DataConverter<T, ID, D, K> converter,
            Class<D> dataType,
            VersionedDataAccessor<D, V> versionAccessor,
            AggregatePersistenceContext persistenceContext) {
        this.mapper = Objects.requireNonNull(mapper, "Mapper must not be null.");
        this.converter = Objects.requireNonNull(converter, "DataConverter must not be null.");
        this.dataType = Objects.requireNonNull(dataType, "Data type must not be null.");
        this.versionAccessor = Objects.requireNonNull(
                versionAccessor, "VersionedDataAccessor must not be null.");
        this.persistenceContext = Objects.requireNonNull(
                persistenceContext, "AggregatePersistenceContext must not be null.");
        this.versionKey = PersistenceStateKey.of(
                dataType.getName() + ".version", versionAccessor.versionType());
    }

    protected final DataConverter<T, ID, D, K> converter() {
        return converter;
    }

    protected final VersionedDataAccessor<D, V> versionAccessor() {
        return versionAccessor;
    }

    protected final AggregatePersistenceContext persistenceContext() {
        return persistenceContext;
    }

    @Override
    protected final T doFindById(ID id) {
        D data = mapper.selectById(converter.toDataId(id));
        if (data == null) {
            return null;
        }
        T aggregate = converter.toEntity(data);
        persistenceContext.attach(aggregate, versionKey, requireVersion(data));
        return aggregate;
    }

    @Override
    protected final void doAdd(T aggregate) {
        D data = converter.toData(aggregate);
        mapper.insert(data);
        persistenceContext.attach(aggregate, versionKey, requireVersion(data));
    }

    @Override
    protected final void doModify(T aggregate) {
        V originalVersion = persistenceContext.require(aggregate, versionKey);
        D data = converter.toData(aggregate);
        versionAccessor.setVersion(data, originalVersion);
        long count = mapper.updateById(data);
        requireVersionedRow(count, "modify", aggregate);
        persistenceContext.replace(aggregate, versionKey, requireVersion(data));
    }

    @Override
    protected final void doRemove(T aggregate) {
        V originalVersion = persistenceContext.require(aggregate, versionKey);
        TableInfo tableInfo = requireTableInfo();
        QueryWrapper<D> wrapper = new QueryWrapper<>();
        wrapper.eq(tableInfo.getKeyColumn(), converter.toDataId(aggregate.getId()));
        wrapper.eq(tableInfo.getVersionFieldInfo().getColumn(), originalVersion);
        long count = mapper.delete(wrapper);
        requireVersionedRow(count, "remove", aggregate);
    }

    private V requireVersion(D data) {
        V version = versionAccessor.getVersion(data);
        if (version == null) {
            throw new IllegalStateException(
                    "Versioned persistence data must provide a version: " + dataType.getName());
        }
        return version;
    }

    private TableInfo requireTableInfo() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(dataType);
        if (tableInfo == null || !tableInfo.havePK() || !tableInfo.isWithVersion()) {
            throw new IllegalStateException(
                    "Versioned persistence data must declare a primary key and @Version field: "
                            + dataType.getName());
        }
        return tableInfo;
    }

    private static void requireVersionedRow(long count, String operation, Object aggregate) {
        if (count == 0) {
            throw new ConflictException(
                    operation + " optimistic lock conflict for aggregate: " + aggregate);
        }
    }
}
