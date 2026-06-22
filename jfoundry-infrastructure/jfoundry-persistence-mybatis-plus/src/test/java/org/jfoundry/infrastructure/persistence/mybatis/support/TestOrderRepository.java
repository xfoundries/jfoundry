package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.infrastructure.persistence.mybatis.MybatisPlusRepository;

/// 测试用仓储:具体 MybatisPlusRepository 子类。
public class TestOrderRepository extends MybatisPlusRepository<TestOrder, TestOrderId, TestOrderData> {

    public TestOrderRepository(TestOrderMapper mapper,
                                DomainEventPublisher eventPublisher,
                                TestOrderDataConverter converter) {
        super(mapper, eventPublisher, converter);
    }
}
