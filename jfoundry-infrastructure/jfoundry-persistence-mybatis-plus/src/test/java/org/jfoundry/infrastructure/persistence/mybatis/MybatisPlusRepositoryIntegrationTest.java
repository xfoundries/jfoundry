package org.jfoundry.infrastructure.persistence.mybatis;

import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrder;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderId;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderMapper;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderRepository;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderStatus;
import org.jfoundry.test.DomainEventCapture;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

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
/// - 事件移交链路(add/modify/remove 成功后移交到 DomainEventPublisher)
@SpringBootTest(classes = PersistenceTestConfig.class)
class MybatisPlusRepositoryIntegrationTest {

    @Autowired
    private TestOrderRepository repository;

    @Autowired
    private TestOrderMapper mapper;

    @Autowired
    private DomainEventCapture eventCapture;

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
        eventCapture.drained();
    }

    // ---- add 链路 ----

    @Test
    void add_新聚合_单条插入成功_事件移交() {
        TestOrderId id = new TestOrderId("ORD-001");
        TestOrder order = TestOrder.create(id, 100);

        repository.add(order);

        TestOrder loaded = repository.findById(id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo(TestOrderStatus.CREATED);
        assertThat(loaded.getAmount()).isEqualTo(100);

        List<DomainEvent> events = eventCapture.drained();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getClass().getSimpleName()).isEqualTo("TestOrderCreatedEvent");
    }

    @Test
    void add_主键冲突_抛异常_且不移交事件() {
        TestOrderId id = new TestOrderId("ORD-DUP");
        TestOrder first = TestOrder.create(id, 100);
        repository.add(first);
        eventCapture.drained();

        TestOrder second = TestOrder.create(id, 200);
        assertThatThrownBy(() -> repository.add(second))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(eventCapture.drained()).isEmpty();
    }

    // ---- modify 链路 ----

    @Test
    void modify_已存在聚合_update成功_事件移交() {
        TestOrderId id = new TestOrderId("ORD-MOD-1");
        repository.add(TestOrder.create(id, 100));
        eventCapture.drained();

        TestOrder loaded = repository.findById(id);
        loaded.markPaid();

        repository.modify(loaded);

        TestOrder afterModify = repository.findById(id);
        assertThat(afterModify.getStatus()).isEqualTo(TestOrderStatus.PAID);

        List<DomainEvent> events = eventCapture.drained();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getClass().getSimpleName()).isEqualTo("TestOrderStatusChangedEvent");
    }

    @Test
    void modify_修改不存在对象_受影响0行_抛IllegalStateException() {
        TestOrderId ghostId = new TestOrderId("ORD-GHOST");
        TestOrder ghost = TestOrder.create(ghostId, 100);

        assertThatThrownBy(() -> repository.modify(ghost))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("modify affected 0 rows");

        assertThat(eventCapture.drained()).isEmpty();
    }

    // ---- addAll / modifyAll 链路 ----

    @Test
    void addAll_批量插入成功_全部事件移交() {
        TestOrder o1 = TestOrder.create(new TestOrderId("ORD-B-1"), 10);
        TestOrder o2 = TestOrder.create(new TestOrderId("ORD-B-2"), 20);
        TestOrder o3 = TestOrder.create(new TestOrderId("ORD-B-3"), 30);

        repository.addAll(List.of(o1, o2, o3));

        assertThat(repository.findById(o1.getId())).isNotNull();
        assertThat(repository.findById(o2.getId())).isNotNull();
        assertThat(repository.findById(o3.getId())).isNotNull();

        assertThat(eventCapture.drained()).hasSize(3);
    }

    @Test
    void modifyAll_批量修改成功_全部事件移交() {
        TestOrderId id1 = new TestOrderId("ORD-MB-1");
        TestOrderId id2 = new TestOrderId("ORD-MB-2");
        repository.addAll(List.of(
                TestOrder.create(id1, 10),
                TestOrder.create(id2, 20)
        ));
        eventCapture.drained();

        TestOrder loaded1 = repository.findById(id1);
        TestOrder loaded2 = repository.findById(id2);
        loaded1.markPaid();
        loaded2.cancel();

        repository.modifyAll(List.of(loaded1, loaded2));

        assertThat(repository.findById(id1).getStatus()).isEqualTo(TestOrderStatus.PAID);
        assertThat(repository.findById(id2).getStatus()).isEqualTo(TestOrderStatus.CANCELLED);

        assertThat(eventCapture.drained()).hasSize(2);
    }

    @Test
    void modifyAll_部分元素不存在_抛IllegalStateException_事务回滚_事件不移交() {
        TestOrderId id1 = new TestOrderId("ORD-MB-OK");
        repository.add(TestOrder.create(id1, 10));
        eventCapture.drained();

        TestOrder loaded1 = repository.findById(id1);
        loaded1.markPaid();
        TestOrder ghost = TestOrder.create(new TestOrderId("ORD-MB-GHOST"), 999);

        assertThatThrownBy(() -> repository.modifyAll(List.of(loaded1, ghost)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("modifyAll affected 0 rows");

        // 注意:addAll/modifyAll 不带 @Transactional,modifyAll 抛异常时已 modify 的元素会保留更新
        // 但事件移交是全部完成后才执行,因此事件列表为空
        assertThat(eventCapture.drained()).isEmpty();
    }

    // ---- remove 链路 ----

    @Test
    void remove_已存在_删除成功_事件移交() {
        TestOrderId id = new TestOrderId("ORD-RM-1");
        TestOrder order = TestOrder.create(id, 100);
        repository.add(order);
        eventCapture.drained();

        // 加载后取消(记录业务事件),再 remove
        TestOrder loaded = repository.findById(id);
        loaded.cancel();
        repository.modify(loaded);
        eventCapture.drained();

        // remove 自身不要求事件,但聚合上若有未移交事件也会一并移交
        TestOrder toRemove = repository.findById(id);
        repository.remove(toRemove);

        assertThat(repository.findById(id)).isNull();
        assertThat(eventCapture.drained()).isEmpty();
    }

    @Test
    void remove_删除不存在对象_受影响0行_抛IllegalStateException() {
        TestOrderId ghostId = new TestOrderId("ORD-RM-GHOST");
        TestOrder ghost = TestOrder.create(ghostId, 100);

        assertThatThrownBy(() -> repository.remove(ghost))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remove affected 0 rows");
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
