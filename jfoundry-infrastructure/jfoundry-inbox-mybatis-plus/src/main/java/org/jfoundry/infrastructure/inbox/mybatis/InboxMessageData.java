package org.jfoundry.infrastructure.inbox.mybatis;

import com.baomidou.mybatisplus.annotation.TableName;
import org.jfoundry.infrastructure.inbox.InboxMessage;
import org.jfoundry.infrastructure.inbox.InboxMessageStatus;

import java.time.Instant;

@TableName("jfoundry_inbox_message")
public class InboxMessageData {

    private String messageId;
    private String consumerName;
    private String status;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    static InboxMessageData processed(String messageId, String consumerName) {
        return fromMessage(InboxMessage.processed(messageId, consumerName));
    }

    static InboxMessageData fromMessage(InboxMessage message) {
        InboxMessageData data = new InboxMessageData();
        data.messageId = message.getMessageId();
        data.consumerName = message.getConsumerName();
        InboxMessageStatus status = message.getStatus();
        data.status = status != null ? status.name() : null;
        data.processedAt = message.getProcessedAt();
        data.createdAt = message.getCreatedAt();
        data.updatedAt = message.getUpdatedAt();
        data.errorMessage = message.getErrorMessage();
        return data;
    }
}
