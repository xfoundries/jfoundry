package org.jfoundry.infrastructure.inbox.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.jfoundry.infrastructure.inbox.InboxMessageStatus;
import org.jfoundry.infrastructure.inbox.InboxRepository;
import org.springframework.dao.DuplicateKeyException;

public class MybatisPlusInboxRepository implements InboxRepository {

    private final InboxMessageMapper mapper;

    public MybatisPlusInboxRepository(InboxMessageMapper mapper) {
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
    public void markProcessed(String messageId, String consumerName) {
        try {
            mapper.insert(InboxMessageData.processed(messageId, consumerName));
        } catch (DuplicateKeyException ignored) {
            // Already processed by this consumer; duplicate delivery stays idempotent.
        }
    }
}
