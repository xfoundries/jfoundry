package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.infrastructure.persistence.AggregatePersistenceContext;
import org.jfoundry.infrastructure.persistence.mybatis.MybatisPlusVersionedAggregateRepository;
import org.jfoundry.infrastructure.persistence.mybatis.VersionedDataAccessor;

public final class TestVersionedOrderRepository extends
        MybatisPlusVersionedAggregateRepository<
                TestOrder, TestOrderId, VersionedOrderData, String, Long> {

    public TestVersionedOrderRepository(
            VersionedOrderMapper mapper,
            VersionedOrderDataConverter converter,
            VersionedDataAccessor<VersionedOrderData, Long> versionAccessor,
            AggregatePersistenceContext persistenceContext) {
        super(mapper, converter, VersionedOrderData.class, versionAccessor, persistenceContext);
    }
}
