package org.jfoundry.application.outbox;

/// Outbox dispatch SPI。实现按 batch size 取出条目并投递。
public interface OutboxDispatcher {

    /// @param batchSize 单次 dispatch 取出的最大条目数
    void dispatch(int batchSize);
}
