package org.jfoundry.infrastructure.inbox.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.jfoundry.application.inbox.InboxMessageStatus;
import org.jfoundry.application.inbox.InboxMessageStore;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;

public class MybatisPlusInboxMessageStore implements InboxMessageStore {

    private final InboxMessageMapper mapper;

    public MybatisPlusInboxMessageStore(InboxMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isProcessed(String messageId, String consumerName) {
        Long count = mapper.selectCount(Wrappers.lambdaQuery(InboxMessageData.class)
                .eq(InboxMessageData::getMessageId, messageId)
                .eq(InboxMessageData::getConsumerName, consumerName)
                .eq(InboxMessageData::getStatus, InboxMessageStatus.PROCESSED.name()));
        return count != null && count > 0;
    }

    @Override
    public boolean tryStartProcessing(String messageId, String consumerName) {
        try {
            mapper.insert(InboxMessageData.processing(messageId, consumerName));
            return true;
        } catch (RuntimeException ex) {
            if (shouldPropagateInsertFailure(ex)) {
                throw ex;
            }
            return retryFailed(messageId, consumerName);
        }
    }

    private boolean retryFailed(String messageId, String consumerName) {
        int updated = mapper.update(null,
                Wrappers.lambdaUpdate(InboxMessageData.class)
                        .set(InboxMessageData::getStatus, InboxMessageStatus.PROCESSING.name())
                        .set(InboxMessageData::getUpdatedAt, Instant.now())
                        .set(InboxMessageData::getErrorMessage, null)
                        .eq(InboxMessageData::getMessageId, messageId)
                        .eq(InboxMessageData::getConsumerName, consumerName)
                        .eq(InboxMessageData::getStatus, InboxMessageStatus.FAILED.name()));
        return updated == 1;
    }

    @Override
    public void markProcessed(String messageId, String consumerName) {
        Instant now = Instant.now();
        int updated = mapper.update(null,
                Wrappers.lambdaUpdate(InboxMessageData.class)
                        .set(InboxMessageData::getStatus, InboxMessageStatus.PROCESSED.name())
                        .set(InboxMessageData::getProcessedAt, now)
                        .set(InboxMessageData::getUpdatedAt, now)
                        .set(InboxMessageData::getErrorMessage, null)
                        .eq(InboxMessageData::getMessageId, messageId)
                        .eq(InboxMessageData::getConsumerName, consumerName)
                        .eq(InboxMessageData::getStatus, InboxMessageStatus.PROCESSING.name()));
        if (updated == 1) {
            return;
        }
        if (!isProcessed(messageId, consumerName)) {
            try {
                mapper.insert(InboxMessageData.processed(messageId, consumerName));
            } catch (RuntimeException ex) {
                if (shouldPropagateInsertFailure(ex)) {
                    throw ex;
                }
                // Already processed or currently owned by another consumer thread.
            }
        }
    }

    @Override
    public void markFailed(String messageId, String consumerName, String errorMessage) {
        Instant now = Instant.now();
        mapper.update(null,
                Wrappers.lambdaUpdate(InboxMessageData.class)
                        .set(InboxMessageData::getStatus, InboxMessageStatus.FAILED.name())
                        .set(InboxMessageData::getUpdatedAt, now)
                        .set(InboxMessageData::getErrorMessage, errorMessage)
                        .eq(InboxMessageData::getMessageId, messageId)
                        .eq(InboxMessageData::getConsumerName, consumerName)
                        .eq(InboxMessageData::getStatus, InboxMessageStatus.PROCESSING.name()));
    }

    private boolean shouldPropagateInsertFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return false;
            }
            String className = current.getClass().getName();
            if (className.endsWith(".DuplicateKeyException")
                    || className.endsWith(".DuplicateKeyExceptionTranslator")) {
                return false;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate")) {
                return false;
            }
            current = current.getCause();
        }
        return true;
    }
}
