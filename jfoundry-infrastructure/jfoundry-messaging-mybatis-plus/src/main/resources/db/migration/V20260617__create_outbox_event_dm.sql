-- DM (达梦) variant of ddd_outbox_event schema.
-- P2-4: mirrors V20260617__create_outbox_event.sql 1:1, except:
--   * payload_json  : MEDIUMTEXT -> CLOB (2GB capacity, DM's native large-string type)
--   * error_message : VARCHAR(2000) retained (DM supports VARCHAR up to ~8KB)
--   * TIMESTAMP     : retained (DM supports ANSI TIMESTAMP)
-- Select via Flyway's databaseId feature or a db-specific profile.
CREATE TABLE ddd_outbox_event (
    event_id        VARCHAR(64)   NOT NULL,
    topic           VARCHAR(255)  NOT NULL,
    payload_key     VARCHAR(255),
    payload_type    VARCHAR(500)  NOT NULL,
    payload_json    CLOB          NOT NULL,  -- P2-4: 2GB capacity
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
