DROP TABLE IF EXISTS test_order;
CREATE TABLE test_order (
    id              VARCHAR(64)   NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    amount          INT           NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    PRIMARY KEY (id)
);
