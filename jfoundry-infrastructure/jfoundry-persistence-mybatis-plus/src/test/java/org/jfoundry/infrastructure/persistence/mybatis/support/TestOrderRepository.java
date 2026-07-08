package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.infrastructure.persistence.mybatis.MybatisPlusRepository;

/// Test repository implemented as a concrete MybatisPlusRepository subclass.
public class TestOrderRepository extends MybatisPlusRepository<TestOrder, TestOrderId, TestOrderData, String> {

    public TestOrderRepository(TestOrderMapper mapper,
                                TestOrderDataConverter converter) {
        super(mapper, converter);
    }
}
