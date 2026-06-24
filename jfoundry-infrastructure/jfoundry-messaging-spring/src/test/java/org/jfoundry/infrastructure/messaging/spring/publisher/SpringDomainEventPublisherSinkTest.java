package org.jfoundry.infrastructure.messaging.spring.publisher;

import org.jfoundry.application.messaging.externalization.DomainEventSink;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SpringDomainEventPublisherSinkTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishInvokesAllSinksAndThenApplicationPublisherWithoutTransaction() {
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        List<DomainEventSink> sinks = List.of(
                new CapturingSink("sink-1"), new CapturingSink("sink-2"));
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(appPublisher, sinks);

        TestDomainEvent event = new TestDomainEvent();
        publisher.publish(event);

        CapturingSink sink1 = (CapturingSink) sinks.get(0);
        CapturingSink sink2 = (CapturingSink) sinks.get(1);
        // Sinks invoked synchronously, in order
        assertThat(sink1.captured).containsExactly(event);
        assertThat(sink2.captured).containsExactly(event);
        // ApplicationEventPublisher ALSO invoked (no transaction context → immediate publish)
        verify(appPublisher).publishEvent(event);
    }

    @Test
    void publishInvokesSinksForEachEvent() {
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        CapturingSink sink = new CapturingSink("sink");
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(appPublisher, List.of(sink));

        TestDomainEvent e1 = new TestDomainEvent();
        TestDomainEvent e2 = new TestDomainEvent();
        publisher.publish(e1, e2);

        assertThat(sink.captured).containsExactly(e1, e2);
    }

    @Test
    void publishWithEmptySinksStillWorks() {
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(appPublisher, List.of());

        publisher.publish(new TestDomainEvent());

        verify(appPublisher).publishEvent(any(DomainEvent.class));
    }

    @Test
    void backwardCompatibleConstructorWorks() {
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(appPublisher);

        publisher.publish(new TestDomainEvent());

        verify(appPublisher).publishEvent(any(DomainEvent.class));
    }

    @Test
    void sinkExceptionPropagatesAndStopsRemainingSinksAndApplicationPublisher() {
        // 锁定 DomainEventSink 的失败语义：sink 抛异常 → 异常向上传播 → 后续 sink 不执行 →
        // ApplicationEventPublisher 也不被调用（异常中断了整个 publish 流程）。
        // 这是 Task A9 Outbox sink 写入失败时回滚业务事务的依赖契约。
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        DomainEventSink throwing = mock(DomainEventSink.class);
        DomainEventSink downstream = mock(DomainEventSink.class);
        doThrow(new RuntimeException("sink boom")).when(throwing).handle(any());
        SpringDomainEventPublisher publisher =
                new SpringDomainEventPublisher(appPublisher, List.of(throwing, downstream));

        TestDomainEvent event = new TestDomainEvent();
        assertThrows(RuntimeException.class, () -> publisher.publish(event));

        verify(throwing).handle(event);
        verifyNoInteractions(downstream);
        verifyNoInteractions(appPublisher);
    }

    @Test
    void publishInvokesSinksBeforeRegisteringAfterCommitCallbackUnderTransaction() {
        // 锁定生产热路径：事务激活时，sink 同步执行，ApplicationEventPublisher 推迟到 afterCommit。
        // 这条路径是 Task A9 Outbox sink 在真实 @Transactional 中实际运行的场景。
        ApplicationEventPublisher appPublisher = mock(ApplicationEventPublisher.class);
        CapturingSink sink = new CapturingSink("sink");
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(appPublisher, List.of(sink));

        TestDomainEvent event = new TestDomainEvent();
        TransactionSynchronizationManager.initSynchronization();
        publisher.publish(event);

        // sink 已同步执行
        assertThat(sink.captured).containsExactly(event);
        // ApplicationEventPublisher 在 afterCommit 之前未调用
        verifyNoInteractions(appPublisher);
        // 注册了恰好一个事务同步
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        // 模拟事务提交
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(appPublisher).publishEvent(event);
    }

    static class CapturingSink implements DomainEventSink {
        final String name;
        final List<DomainEvent> captured = new ArrayList<>();

        CapturingSink(String name) { this.name = name; }

        @Override
        public void handle(DomainEvent event) { captured.add(event); }
    }

    static class TestDomainEvent implements DomainEvent {}
}
