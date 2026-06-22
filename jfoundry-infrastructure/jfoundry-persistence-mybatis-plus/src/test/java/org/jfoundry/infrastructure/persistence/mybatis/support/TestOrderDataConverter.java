package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.infrastructure.persistence.DataConverter;

/// 测试用聚合根 ↔ Data 转换器。
public class TestOrderDataConverter implements DataConverter<TestOrder, TestOrderId, TestOrderData> {

    @Override
    public TestOrderData toData(TestOrder entity) {
        TestOrderData data = new TestOrderData();
        data.setId(entity.getId());
        data.setStatus(entity.getStatus().name());
        data.setAmount(entity.getAmount());
        data.setCreatedAt(entity.getCreatedAt());
        data.setUpdatedAt(entity.getUpdatedAt());
        return data;
    }

    @Override
    public TestOrder toEntity(TestOrderData data) {
        return TestOrder.restore(
                data.getId(),
                TestOrderStatus.valueOf(data.getStatus()),
                data.getAmount(),
                data.getCreatedAt(),
                data.getUpdatedAt()
        );
    }
}
