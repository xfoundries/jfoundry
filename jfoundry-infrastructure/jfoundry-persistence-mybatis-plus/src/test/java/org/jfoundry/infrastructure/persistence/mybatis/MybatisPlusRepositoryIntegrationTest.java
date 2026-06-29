package org.jfoundry.infrastructure.persistence.mybatis;

import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrder;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderId;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderMapper;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderRepository;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderStatus;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.TypeVariable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// MybatisPlusRepository 集成测试。
/// <p>
/// 用 H2 embedded 跑全链路,覆盖:
/// - add / addAll 新建链路 + 主键冲突防御
/// - modify / modifyAll 更新链路 + 0 行防御
/// - remove 删除链路 + 0 行防御
/// - findById 加载链路
/// - 上下文注册链接(add/modify/remove 成功后向 DomainEventContext 注册聚合)
@SpringBootTest(classes = PersistenceTestConfig.class)
class MybatisPlusRepositoryIntegrationTest {

    @Autowired
    private TestOrderRepository repository;

    @Autowired
    private TestOrderMapper mapper;

    @Autowired
    private PersistenceTestConfig.TestDomainEventContext domainEventContext;

    @Test
    void mybatisPlusRepositoryShouldUseJavaStyleTypeParameterNames() {
        assertThat(typeParameterNames(MybatisPlusRepository.class)).containsExactly("T", "ID", "D", "K");
    }

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
        domainEventContext.drainRegistered();
    }

    private static List<String> typeParameterNames(Class<?> type) {
        return List.of(type.getTypeParameters()).stream().map(TypeVariable::getName).toList();
    }

    // ---- add 链路 ----

    @Test
    void add_新聚合_单条插入成功_注册聚合且不清空事件() {
        TestOrderId id = new TestOrderId("ORD-001");
        TestOrder order = TestOrder.create(id, 100);
        List<DomainEvent> expectedEvents = order.drainEvents();
        assertThat(expectedEvents).hasSize(1);
        TestOrder orderWithEvents = TestOrder.create(id, 100);

        repository.add(orderWithEvents);

        TestOrder loaded = repository.findById(id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo(TestOrderStatus.CREATED);
        assertThat(loaded.getAmount()).isEqualTo(100);

        assertThat(domainEventContext.drainRegistered()).containsExactly(orderWithEvents);
        assertThat(orderWithEvents.drainEvents())
                .singleElement()
                .isInstanceOf(expectedEvents.getFirst().getClass());
    }

    @Test
    void add_主键冲突_抛异常_且不注册聚合() {
        TestOrderId id = new TestOrderId("ORD-DUP");
        TestOrder first = TestOrder.create(id, 100);
        repository.add(first);
        domainEventContext.drainRegistered();

        TestOrder second = TestOrder.create(id, 200);
        assertThatThrownBy(() -> repository.add(second))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(domainEventContext.drainRegistered()).isEmpty();
        assertThat(second.drainEvents()).hasSize(1);
    }

    // ---- modify 链路 ----

    @Test
    void modify_已存在聚合_update成功_注册聚合且不清空事件() {
        TestOrderId id = new TestOrderId("ORD-MOD-1");
        repository.add(TestOrder.create(id, 100));
        domainEventContext.drainRegistered();

        TestOrder loaded = repository.findById(id);
        loaded.markPaid();

        repository.modify(loaded);

        TestOrder afterModify = repository.findById(id);
        assertThat(afterModify.getStatus()).isEqualTo(TestOrderStatus.PAID);

        assertThat(domainEventContext.drainRegistered()).containsExactly(loaded);
        assertThat(loaded.drainEvents())
                .singleElement()
                .isInstanceOfSatisfying(DomainEvent.class,
                        event -> assertThat(event.getClass().getSimpleName()).isEqualTo("TestOrderStatusChangedEvent"));
    }

    @Test
    void modify_修改不存在对象_受影响0行_抛IllegalStateException() {
        TestOrderId ghostId = new TestOrderId("ORD-GHOST");
        TestOrder ghost = TestOrder.create(ghostId, 100);

        assertThatThrownBy(() -> repository.modify(ghost))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("modify affected 0 rows");

        assertThat(domainEventContext.drainRegistered()).isEmpty();
        assertThat(ghost.drainEvents()).hasSize(1);
    }

    // ---- addAll / modifyAll 链路 ----

    @Test
    void addAll_批量插入成功_按顺序注册全部聚合且不清空事件() {
        TestOrder o1 = TestOrder.create(new TestOrderId("ORD-B-1"), 10);
        TestOrder o2 = TestOrder.create(new TestOrderId("ORD-B-2"), 20);
        TestOrder o3 = TestOrder.create(new TestOrderId("ORD-B-3"), 30);

        repository.addAll(List.of(o1, o2, o3));

        assertThat(repository.findById(o1.getId())).isNotNull();
        assertThat(repository.findById(o2.getId())).isNotNull();
        assertThat(repository.findById(o3.getId())).isNotNull();

        assertThat(domainEventContext.drainRegistered()).containsExactly(o1, o2, o3);
        assertThat(o1.drainEvents()).hasSize(1);
        assertThat(o2.drainEvents()).hasSize(1);
        assertThat(o3.drainEvents()).hasSize(1);
    }

    @Test
    void modifyAll_批量修改成功_按顺序注册全部聚合且不清空事件() {
        TestOrderId id1 = new TestOrderId("ORD-MB-1");
        TestOrderId id2 = new TestOrderId("ORD-MB-2");
        repository.addAll(List.of(
                TestOrder.create(id1, 10),
                TestOrder.create(id2, 20)
        ));
        domainEventContext.drainRegistered();

        TestOrder loaded1 = repository.findById(id1);
        TestOrder loaded2 = repository.findById(id2);
        loaded1.markPaid();
        loaded2.cancel();

        repository.modifyAll(List.of(loaded1, loaded2));

        assertThat(repository.findById(id1).getStatus()).isEqualTo(TestOrderStatus.PAID);
        assertThat(repository.findById(id2).getStatus()).isEqualTo(TestOrderStatus.CANCELLED);

        assertThat(domainEventContext.drainRegistered()).containsExactly(loaded1, loaded2);
        assertThat(loaded1.drainEvents()).hasSize(1);
        assertThat(loaded2.drainEvents()).hasSize(1);
    }

    @Test
    void 同一聚合在同一上下文内多次持久化_只注册一次且事件持续累积() {
        TestOrderId id = new TestOrderId("ORD-DEDUP-1");
        TestOrder order = TestOrder.create(id, 100);

        repository.add(order);
        order.markPaid();
        repository.modify(order);

        assertThat(domainEventContext.drainRegistered()).containsExactly(order);
        assertThat(order.drainEvents()).hasSize(2);
    }

    @Test
    void modifyAll_部分元素不存在_抛IllegalStateException_聚合不注册且事件保留() {
        TestOrderId id1 = new TestOrderId("ORD-MB-OK");
        repository.add(TestOrder.create(id1, 10));
        domainEventContext.drainRegistered();

        TestOrder loaded1 = repository.findById(id1);
        loaded1.markPaid();
        TestOrder ghost = TestOrder.create(new TestOrderId("ORD-MB-GHOST"), 999);

        assertThatThrownBy(() -> repository.modifyAll(List.of(loaded1, ghost)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("modifyAll affected 0 rows");

        assertThat(domainEventContext.drainRegistered()).isEmpty();
        assertThat(loaded1.drainEvents()).hasSize(1);
        assertThat(ghost.drainEvents()).hasSize(1);
    }

    // ---- remove 链路 ----

    @Test
    void remove_已存在_删除成功_注册聚合且不清空事件() {
        TestOrderId id = new TestOrderId("ORD-RM-1");
        TestOrder order = TestOrder.create(id, 100);
        repository.add(order);
        domainEventContext.drainRegistered();

        TestOrder toRemove = repository.findById(id);
        toRemove.cancel();
        repository.remove(toRemove);

        assertThat(repository.findById(id)).isNull();
        assertThat(domainEventContext.drainRegistered()).containsExactly(toRemove);
        assertThat(toRemove.drainEvents())
                .singleElement()
                .isInstanceOfSatisfying(DomainEvent.class,
                        event -> assertThat(event.getClass().getSimpleName()).isEqualTo("TestOrderStatusChangedEvent"));
    }

    @Test
    void remove_删除不存在对象_受影响0行_抛IllegalStateException() {
        TestOrderId ghostId = new TestOrderId("ORD-RM-GHOST");
        TestOrder ghost = TestOrder.create(ghostId, 100);

        assertThatThrownBy(() -> repository.remove(ghost))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remove affected 0 rows");

        assertThat(domainEventContext.drainRegistered()).isEmpty();
        assertThat(ghost.drainEvents()).hasSize(1);
    }

    // ---- findById 链路 ----

    @Test
    void findById_存在_返回聚合() {
        TestOrderId id = new TestOrderId("ORD-FIND-1");
        repository.add(TestOrder.create(id, 100));

        TestOrder loaded = repository.findById(id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(id);
    }

    @Test
    void findById_不存在_返回null() {
        assertThat(repository.findById(new TestOrderId("ORD-NOPE"))).isNull();
    }

    @Test
    void findById_null参数_抛IllegalArgumentException() {
        assertThatThrownBy(() -> repository.findById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
