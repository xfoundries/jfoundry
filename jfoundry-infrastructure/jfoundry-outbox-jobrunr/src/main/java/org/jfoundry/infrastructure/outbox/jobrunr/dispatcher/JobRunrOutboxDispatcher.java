package org.jfoundry.infrastructure.outbox.jobrunr.dispatcher;

import org.jfoundry.infrastructure.messaging.MessageSender;
import org.jfoundry.infrastructure.outbox.core.BackoffStrategy;
import org.jfoundry.infrastructure.outbox.core.DefaultOutboxDispatchService;
import org.jfoundry.infrastructure.outbox.core.OutboxDispatcher;
import org.jfoundry.infrastructure.outbox.core.OutboxRepository;
import org.jfoundry.infrastructure.outbox.core.OutboxRuntimeIds;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// JobRunr trigger for the framework-neutral Outbox dispatch runtime.
public class JobRunrOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobRunrOutboxDispatcher.class);

    private final OutboxDispatcher dispatchService;
    private final int batchSize;

    public JobRunrOutboxDispatcher(OutboxRepository outboxRepository,
                                    MessageSender messageSender,
                                    int batchSize,
                                    int maxRetries,
                                    BackoffStrategy backoff) {
        this(outboxRepository, messageSender, batchSize, maxRetries, backoff,
                OutboxRuntimeIds.generateClaimerId());
    }

    /// 测试专用：允许注入 podId 以断言并发互斥行为。生产构造函数走
    /// {@link OutboxRuntimeIds#generateClaimerId()}。
    JobRunrOutboxDispatcher(OutboxRepository outboxRepository,
                            MessageSender messageSender,
                            int batchSize,
                            int maxRetries,
                            BackoffStrategy backoff,
                            String podId) {
        this.dispatchService = new DefaultOutboxDispatchService(
                outboxRepository, messageSender, maxRetries, backoff, podId);
        this.batchSize = batchSize;
    }

    @Job(name = "outbox-dispatch", retries = 3)
    public void recurringDispatch() {
        dispatch(batchSize);
    }

    @Override
    public void dispatch(int batchSize) {
        log.debug("[OUTBOX-DISPATCH-JOBRUNR] dispatching batchSize={}", batchSize);
        dispatchService.dispatch(batchSize);
    }
}
