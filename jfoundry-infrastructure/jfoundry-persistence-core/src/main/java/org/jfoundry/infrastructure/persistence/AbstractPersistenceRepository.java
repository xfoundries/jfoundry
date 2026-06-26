package org.jfoundry.infrastructure.persistence;

import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/// 持久化仓储抽象基类(模板方法模式)。
/// <p>
/// 骨架逻辑:
/// - 上下文登记:add/modify/addAll/modifyAll/remove 成功后向 DomainEventContext 注册聚合
/// - 防御契约:modify/remove 受影响行数为 0 时抛 IllegalStateException,避免沉默失败
/// - addAll/modifyAll 批量处理:逐个聚合登记上下文
/// <p>
/// 子类需要实现的模板方法:
/// - {@link #insertData}:单条新增
/// - {@link #updateData}:单条更新,返回受影响行数
/// - {@link #deleteDataById}:按 ID 删除,返回受影响行数
/// - {@link #selectDataById}:按 ID 查询
/// <p>
/// 具体实现示例:{@code jfoundry-persistence-mybatis-plus} 模块的 {@code MybatisPlusRepository}(基于 MyBatis-Plus BaseMapper)。
/// 未来可扩展 JPA / Mongo 等同位模块。
///
/// @param <T>  聚合根类型,必须同时是 jMolecules AggregateRoot 和 framework EventRecordable
/// @param <ID> 标识符类型
/// @param <D>  数据库实体类型
public abstract class AbstractPersistenceRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends AggregateData<ID>>
        implements AggregateRepository<T, ID> {

    private final DomainEventContext domainEventContext;
    private final DataConverter<T, ID, D> converter;

    protected AbstractPersistenceRepository(DomainEventContext domainEventContext,
                                             DataConverter<T, ID, D> converter) {
        this.domainEventContext = domainEventContext;
        this.converter = converter;
    }

    /// 子类实现的模板方法:单条新增。
    protected abstract void insertData(D data);

    /// 子类实现的模板方法:单条更新,返回受影响行数(用于 modify 校验沉默失败)。
    protected abstract long updateData(D data);

    /// 子类实现的模板方法:按 ID 删除,返回受影响行数(用于 remove 校验沉默失败)。
    protected abstract long deleteDataById(ID id);

    /// 子类实现的模板方法:按 ID 查询,返回 null 表示不存在。
    protected abstract D selectDataById(ID id);

    /// 子类可访问的 converter(用于 findById 转换)。
    protected DataConverter<T, ID, D> converter() {
        return converter;
    }

    @Override
    public T findById(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity id must not be null.");
        }
        D data = selectDataById(id);
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
        long count = deleteDataById(entityId);
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

    /// 持久化层只登记成功持久化的聚合，由应用层在用例边界统一提取并分发事件。
    private void registerAggregate(T entity) {
        if (domainEventContext == null) {
            return;
        }
        domainEventContext.register(entity);
    }
}
