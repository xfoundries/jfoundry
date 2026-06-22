package org.jfoundry.infrastructure.persistence.mybatis.support;

import org.jfoundry.domain.entity.agg.BaseAggregateRoot;
import org.jmolecules.ddd.types.AggregateRoot;

import java.time.Instant;

/// 测试用聚合根。体现典型的"业务方法决定记录什么事件"模式:
/// - {@link #markPaid} 是修改语义,记录 TestOrderStatusChangedEvent
/// - {@link #cancel} 是修改语义,记录 TestOrderStatusChangedEvent
public class TestOrder extends BaseAggregateRoot<TestOrder, TestOrderId> implements AggregateRoot<TestOrder, TestOrderId> {

    private TestOrderStatus status;
    private int amount;
    private Instant createdAt;
    private Instant updatedAt;

    /// 新建聚合的工厂方法(assigned-ID 模式)。构造时记录 CreatedEvent。
    public static TestOrder create(TestOrderId id, int amount) {
        TestOrder order = new TestOrder(id);
        order.status = TestOrderStatus.CREATED;
        order.amount = amount;
        Instant now = Instant.now();
        order.createdAt = now;
        order.updatedAt = now;
        order.recordEvent(new TestOrderCreatedEvent(id.value(), now));
        return order;
    }

    /// 持久化重建构造器(从 Data 还原,不记录事件)。
    public static TestOrder restore(TestOrderId id, TestOrderStatus status, int amount, Instant createdAt, Instant updatedAt) {
        TestOrder order = new TestOrder(id);
        order.status = status;
        order.amount = amount;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    private TestOrder(TestOrderId id) {
        super(id);
    }

    /// 业务方法:标记为已支付。修改语义,记录 StatusChangedEvent。
    public void markPaid() {
        transitionTo(TestOrderStatus.PAID);
    }

    /// 业务方法:取消订单。修改语义,记录 StatusChangedEvent。
    public void cancel() {
        transitionTo(TestOrderStatus.CANCELLED);
    }

    private void transitionTo(TestOrderStatus next) {
        TestOrderStatus previous = this.status;
        this.status = next;
        this.updatedAt = Instant.now();
        recordEvent(new TestOrderStatusChangedEvent(getId().value(), previous.name(), next.name(), updatedAt));
    }

    public TestOrderStatus getStatus() {
        return status;
    }

    public int getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
