CREATE TABLE ddd_outbox_event (
    event_id        VARCHAR(64)   NOT NULL,
    topic           VARCHAR(255)  NOT NULL,
    payload_key     VARCHAR(255),
    payload_type    VARCHAR(500)  NOT NULL,
    payload_json    TEXT          NOT NULL,
    -- PENDING / DISPATCHING / PUBLISHED / FAILED / DEAD_LETTERED
    status          VARCHAR(32)   NOT NULL,
    retry_count     INT           NOT NULL DEFAULT 0,
    error_message   VARCHAR(2000),
    occurred_at     TIMESTAMP     NOT NULL,
    last_attempt_at TIMESTAMP,
    next_retry_at   TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    -- P2-1: atomic claim columns (DISPATCHING state)
    claimed_at      TIMESTAMP,
    claimed_by      VARCHAR(100),
    PRIMARY KEY (event_id)
);
CREATE INDEX idx_outbox_status_retry ON ddd_outbox_event (status, next_retry_at);
-- P2-1: composite index for atomic claimDispatchable WHERE clause
CREATE INDEX idx_outbox_claim ON ddd_outbox_event (status, claimed_at);
