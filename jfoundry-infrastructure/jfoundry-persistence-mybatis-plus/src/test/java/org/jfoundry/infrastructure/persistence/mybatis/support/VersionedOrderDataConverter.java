package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.infrastructure.persistence.DataConverter;

public final class VersionedOrderDataConverter
        implements DataConverter<TestOrder, TestOrderId, VersionedOrderData, String> {

    @Override
    public VersionedOrderData toData(TestOrder entity) {
        VersionedOrderData data = new VersionedOrderData();
        data.setId(toDataId(entity.getId()));
        data.setStatus(entity.getStatus().name());
        data.setAmount(entity.getAmount());
        data.setCreatedAt(entity.getCreatedAt());
        data.setUpdatedAt(entity.getUpdatedAt());
        return data;
    }

    @Override
    public TestOrder toEntity(VersionedOrderData data) {
        return TestOrder.restore(
                new TestOrderId(data.getId()),
                TestOrderStatus.valueOf(data.getStatus()),
                data.getAmount(),
                data.getCreatedAt(),
                data.getUpdatedAt());
    }

    @Override
    public String toDataId(TestOrderId id) {
        return id == null ? null : id.value();
    }
}
