package org.jfoundry.infrastructure.outbox.jobrunr.dispatcher;

import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.DefaultOutboxDispatchService;
import org.jfoundry.application.outbox.OutboxDispatcher;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.application.outbox.OutboxRuntimeIds;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// JobRunr trigger for the framework-neutral Outbox dispatch runtime.
public class JobRunrOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobRunrOutboxDispatcher.class);

    private final OutboxDispatcher dispatchService;
    private final int batchSize;

    public JobRunrOutboxDispatcher(OutboxMessageStore outboxRepository,
                                    MessageSender messageSender,
                                    int batchSize,
                                    int maxRetries,
                                    BackoffStrategy backoff) {
        this(outboxRepository, messageSender, batchSize, maxRetries, backoff,
                OutboxRuntimeIds.generateClaimerId());
    }

    /// 测试专用：允许注入 podId 以断言并发互斥行为。生产构造函数走
    /// {@link OutboxRuntimeIds#generateClaimerId()}。
    JobRunrOutboxDispatcher(OutboxMessageStore outboxRepository,
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
