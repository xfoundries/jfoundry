package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.application.event.DomainEventContext;
import org.jfoundry.infrastructure.persistence.mybatis.MybatisPlusRepository;

/// 测试用仓储:具体 MybatisPlusRepository 子类。
public class TestOrderRepository extends MybatisPlusRepository<TestOrder, TestOrderId, TestOrderData, String> {

    public TestOrderRepository(TestOrderMapper mapper,
                                DomainEventContext domainEventContext,
                                TestOrderDataConverter converter) {
        super(mapper, domainEventContext, converter);
    }
}
