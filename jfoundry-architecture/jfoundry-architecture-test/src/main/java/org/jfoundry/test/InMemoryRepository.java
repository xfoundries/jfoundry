package org.jfoundry.test;

import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// 内存仓储,业务测试时复用。
/// <p>
/// 用 {@link HashMap} 模拟聚合集合,提供 {@link #add} / {@link #modify} / {@link #remove} 集合三件套,
/// 事件移交语义与 {@code MybatisPlusRepository} 保持一致(移交后立即 clearEvents)。
/// <p>
/// 防御契约与持久化实现一致:
/// - {@link #add}:重复 add 同一 ID 抛 {@link IllegalStateException}(模拟主键冲突)
/// - {@link #modify}:集合中不存在该 ID 抛 {@link IllegalStateException}(模拟"修改了不存在对象")
/// - {@link #remove}:集合中不存在该 ID 抛 {@link IllegalStateException}(模拟"删除了不存在对象")
///
/// @param <T>  聚合根类型
/// @param <ID> 标识符类型
public class InMemoryRepository<T extends AggregateRoot<T, ID> & EventRecordable, ID extends Identifier>
        implements AggregateRepository<T, ID> {

    private final Map<ID, T> entities = new HashMap<>();

    @Override
    public T findById(ID id) {
        return entities.get(id);
    }

    @Override
    public void add(T entity) {
        ID id = entity.getId();
        if (entities.containsKey(id)) {
            throw new IllegalStateException("add failed — entity already exists: " + id);
        }
        entities.put(id, entity);
    }

    @Override
    public void modify(T entity) {
        ID id = entity.getId();
        if (!entities.containsKey(id)) {
            throw new IllegalStateException("modify failed — entity not found: " + id);
        }
        entities.put(id, entity);
    }

    @Override
    public void addAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (T entity : entities) {
            add(entity);
        }
    }

    @Override
    public void modifyAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (T entity : entities) {
            modify(entity);
        }
    }

    @Override
    public void remove(T entity) {
        ID id = entity.getId();
        if (!entities.containsKey(id)) {
            throw new IllegalStateException("remove failed — entity not found: " + id);
        }
        entities.remove(id);
    }

    public Optional<T> tryFind(ID id) {
        return Optional.ofNullable(entities.get(id));
    }

    public List<T> all() {
        return List.copyOf(entities.values());
    }

    public void clear() {
        entities.clear();
    }
}
