package org.jfoundry.infrastructure.persistence;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.event.types.DomainEvent;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/// 持久化仓储抽象基类(模板方法模式)。
/// <p>
/// 骨架逻辑:
/// - 事件移交:add/modify/addAll/modifyAll/remove 成功后读取聚合事件,调用 DomainEventPublisher 发布,并立即清空聚合事件
/// - 防御契约:modify/remove 受影响行数为 0 时抛 IllegalStateException,避免沉默失败
/// - addAll/modifyAll 批量处理:逐个聚合移交事件
/// <p>
/// 子类需要实现的模板方法:
/// - {@link #insertData}:单条新增
/// - {@link #updateData}:单条更新,返回受影响行数
/// - {@link #deleteDataById}:按 ID 删除,返回受影响行数
/// - {@link #selectDataById}:按 ID 查询
/// <p>
/// 具体实现示例:{@code ddd-persistence-mybatis-plus} 模块的 {@code MybatisPlusRepository}(基于 MyBatis-Plus BaseMapper)。
/// 未来可扩展 JPA / Mongo 等同位模块。
///
/// @param <T>  聚合根类型,必须同时是 jMolecules AggregateRoot 和 framework EventRecordable
/// @param <ID> 标识符类型
/// @param <D>  数据库实体类型
public abstract class AbstractPersistenceRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends BaseData<ID>>
        implements AggregateRepository<T, ID> {

    private final DomainEventPublisher eventPublisher;
    private final DataConverter<T, ID, D> converter;

    protected AbstractPersistenceRepository(DomainEventPublisher eventPublisher,
                                             DataConverter<T, ID, D> converter) {
        this.eventPublisher = eventPublisher;
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
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        D data = converter.toData(entity);
        insertData(data);
        handoverEvents(entity);
    }

    @Override
    public void modify(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        D data = converter.toData(entity);
        long count = updateData(data);
        if (count == 0) {
            throw new IllegalStateException(
                    "modify affected 0 rows — entity not found or optimistic lock conflict: " + entity.getId());
        }
        handoverEvents(entity);
    }

    @Override
    public void addAll(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities must not be null.");
        }
        if (entities.isEmpty()) {
            return;
        }
        for (T entity : entities) {
            if (entity == null) {
                throw new IllegalArgumentException("Entities must not contain null.");
            }
        }
        List<T> entityList = List.copyOf(entities);
        for (T entity : entityList) {
            insertData(converter.toData(entity));
        }
        entityList.forEach(this::handoverEvents);
    }

    @Override
    public void modifyAll(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Entities must not be null.");
        }
        if (entities.isEmpty()) {
            return;
        }
        for (T entity : entities) {
            if (entity == null) {
                throw new IllegalArgumentException("Entities must not contain null.");
            }
        }
        List<T> entityList = List.copyOf(entities);
        for (T entity : entityList) {
            D data = converter.toData(entity);
            long count = updateData(data);
            if (count == 0) {
                throw new IllegalStateException(
                        "modifyAll affected 0 rows — entity not found or optimistic lock conflict: " + entity.getId());
            }
        }
        entityList.forEach(this::handoverEvents);
    }

    @Override
    public void remove(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Entity id must not be null.");
        }
        long count = deleteDataById(entity.getId());
        if (count == 0) {
            throw new IllegalStateException(
                    "remove affected 0 rows — entity not found: " + entity.getId());
        }
        handoverEvents(entity);
    }

    /// 事件移交语义:
    /// - 无 publisher 时不清空聚合事件
    /// - 无事件时不调用 publisher
    /// - 移交后立即 clearEvents()
    private void handoverEvents(T entity) {
        if (eventPublisher == null) {
            return;
        }
        List<DomainEvent> events = entity.getEvents();
        if (events.isEmpty()) {
            return;
        }
        eventPublisher.publish(events.toArray(DomainEvent[]::new));
        entity.clearEvents();
    }
}
