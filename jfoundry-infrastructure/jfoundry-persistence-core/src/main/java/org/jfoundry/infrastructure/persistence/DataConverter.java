package org.jfoundry.infrastructure.persistence;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.List;

/// 数据转换器。
///
/// 在领域聚合根 (T) 与持久化数据对象 (D) 之间转换。
/// 实现类只需提供单对象转换；批量转换基于单对象转换默认实现。
///
/// @param <T>  聚合根类型
/// @param <ID> 标识符类型
/// @param <D>  数据对象类型
public interface DataConverter<
        T extends AggregateRoot<T, ID>,
        ID extends Identifier & Serializable,
        D extends AggregateData<ID>> {

    /// 将聚合根转换为数据对象。
    D toData(T entity);

    /// 将数据对象转换为聚合根。
    T toEntity(D data);

    /// 将聚合根列表转换为数据对象列表。
    default List<D> toDataList(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(this::toData).toList();
    }

    /// 将数据对象列表转换为聚合根列表。
    default List<T> toEntityList(List<D> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return List.of();
        }
        return dataList.stream().map(this::toEntity).toList();
    }
}
