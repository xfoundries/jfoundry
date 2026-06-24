package org.jfoundry.infrastructure.outbox.spring.dispatcher;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.DefaultOutboxDispatchService;
import org.jfoundry.application.outbox.OutboxDispatcher;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.application.outbox.OutboxRuntimeIds;
import org.springframework.scheduling.annotation.Scheduled;

/// Spring scheduled trigger for the framework-neutral Outbox dispatch runtime.
public class ScheduledOutboxDispatcher implements OutboxDispatcher {

    private final OutboxDispatcher dispatchService;
    private final int batchSize;

    public ScheduledOutboxDispatcher(OutboxMessageStore repository,
                                      MessageSender messageSender,
                                      int maxRetries,
                                      BackoffStrategy backoff,
                                      int batchSize) {
        this(repository, messageSender, maxRetries, backoff, batchSize, OutboxRuntimeIds.generateClaimerId());
    }

    /// 测试专用：允许注入 podId 以断言并发互斥行为。生产构造函数走
    /// {@link OutboxRuntimeIds#generateClaimerId()}。
    public ScheduledOutboxDispatcher(OutboxMessageStore repository,
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
