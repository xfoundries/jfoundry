package org.jfoundry.infrastructure.persistence.mybatis;

/// Reads and assigns an optimistic-lock version on a persistence data object.
///
/// @param <D> persistence data type
/// @param <V> version type
public interface VersionedDataAccessor<D, V> {

    Class<V> versionType();

    V getVersion(D data);

    void setVersion(D data, V version);
}
