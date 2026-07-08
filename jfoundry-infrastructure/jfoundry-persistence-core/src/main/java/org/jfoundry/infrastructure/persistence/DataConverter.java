package org.jfoundry.infrastructure.persistence;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;
import java.util.List;

/// Data converter.
///
/// Converts between domain aggregate roots (T) and persistence data objects (D).
/// Implementations only need to provide single-object conversion; batch conversion is derived from
/// those methods by default.
///
/// @param <T>  aggregate root type
/// @param <ID> domain identifier type
/// @param <D>  data object type
/// @param <K>  persistence primary-key type
public interface DataConverter<
        T extends AggregateRoot<T, ID>,
        ID extends Identifier & Serializable,
        D extends AggregateData<K>,
        K extends Serializable> {

    /// Converts an aggregate root to a data object.
    D toData(T entity);

    /// Converts a data object to an aggregate root.
    T toEntity(D data);

    /// Converts a domain identifier to a persistence identifier.
    K toDataId(ID id);

    /// Converts aggregate roots to data objects.
    default List<D> toDataList(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(this::toData).toList();
    }

    /// Converts data objects to aggregate roots.
    default List<T> toEntityList(List<D> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return List.of();
        }
        return dataList.stream().map(this::toEntity).toList();
    }
}
