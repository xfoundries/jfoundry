DROP TABLE IF EXISTS ddd_outbox_event;
CREATE TABLE ddd_outbox_event (
    event_id        VARCHAR(64)   NOT NULL,
    topic           VARCHAR(255)  NOT NULL,
    payload_key     VARCHAR(255),
    payload_type    VARCHAR(500)  NOT NULL,
    payload_json    TEXT          NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    retry_count     INT           NOT NULL DEFAULT 0,
    error_message   VARCHAR(2000),
    occurred_at     TIMESTAMP     NOT NULL,
    last_attempt_at TIMESTAMP,
    next_retry_at   TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    PRIMARY KEY (event_id)
);
