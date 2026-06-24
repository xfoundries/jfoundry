DROP TABLE IF EXISTS jfoundry_inbox_message;
CREATE TABLE jfoundry_inbox_message (
    message_id    VARCHAR(128)  NOT NULL,
    consumer_name VARCHAR(255)  NOT NULL,
    status        VARCHAR(32)   NOT NULL,
    processed_at  TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    error_message VARCHAR(2000),
    PRIMARY KEY (consumer_name, message_id)
);
CREATE INDEX idx_inbox_processed_at ON jfoundry_inbox_message (processed_at);
