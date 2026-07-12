package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.infrastructure.persistence.mybatis.VersionedDataAccessor;

public final class TestOrderVersionAccessor
        implements VersionedDataAccessor<VersionedOrderData, Long> {

    @Override
    public Class<Long> versionType() {
        return Long.class;
    }

    @Override
    public Long getVersion(VersionedOrderData data) {
        return data.getVersion();
    }

    @Override
    public void setVersion(VersionedOrderData data, Long version) {
        data.setVersion(version);
    }
}
