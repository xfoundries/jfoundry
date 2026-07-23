package org.jfoundry.application.outbox;

/// Outbox dispatch SPI. Implementations claim and deliver entries by batch size.
public interface OutboxDispatcher {

    /// @param batchSize maximum number of entries claimed by one dispatch run
    void dispatch(int batchSize);
}
