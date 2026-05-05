-- V1: Initial schema for transaction_db
-- TransactionService owns the 'transactions' table

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    account_number   VARCHAR(20)    NOT NULL,
    amount           DECIMAL(19, 4) NOT NULL,
    message          VARCHAR(500),
    transaction_type VARCHAR(10)    NOT NULL,
    idempotency_key  VARCHAR(36)    UNIQUE,
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_txn_account_number (account_number),
    INDEX idx_txn_type           (transaction_type),
    INDEX idx_txn_created_at     (created_at),
    UNIQUE KEY uk_idempotency_key (idempotency_key),

    CONSTRAINT chk_txn_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_txn_type CHECK (transaction_type IN ('CREDIT', 'DEBIT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

