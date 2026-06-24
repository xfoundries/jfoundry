package org.jfoundry.infrastructure.outbox.spring.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.DefaultOutboxDispatchService;
import org.jfoundry.infrastructure.outbox.core.OutboxDispatcher;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jfoundry.infrastructure.outbox.core.OutboxRuntimeIds;
import org.springframework.scheduling.annotation.Scheduled;

/// Spring scheduled trigger for the framework-neutral Outbox dispatch runtime.
public class ScheduledOutboxDispatcher implements OutboxDispatcher {

    private final OutboxDispatcher dispatchService;
    private final int batchSize;

    public ScheduledOutboxDispatcher(OutboxRepository repository,
                                      MessageSender messageSender,
                                      int maxRetries,
                                      BackoffStrategy backoff,
                                      int batchSize) {
        this(repository, messageSender, maxRetries, backoff, batchSize, OutboxRuntimeIds.generateClaimerId());
    }

    /// 测试专用：允许注入 podId 以断言并发互斥行为。生产构造函数走
    /// {@link OutboxRuntimeIds#generateClaimerId()}。
    public ScheduledOutboxDispatcher(OutboxRepository repository,
                                     MessageSender messageSender,
                                     int maxRetries,
                                     BackoffStrategy backoff,
                                     int batchSize,
                                     String podId) {
        this.dispatchService = new DefaultOutboxDispatchService(repository, messageSender, maxRetries, backoff, podId);
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${jfoundry.outbox.dispatcher.interval-ms:5000}")
    public void scheduledDispatch() {
        dispatch(batchSize);
    }

    @Override
    public void dispatch(int batchSize) {
        dispatchService.dispatch(batchSize);
    }
}
