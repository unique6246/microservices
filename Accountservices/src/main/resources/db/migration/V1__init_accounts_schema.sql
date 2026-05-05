-- V1: Initial schema for accounts_db
-- AccountService owns the 'accounts' table

CREATE TABLE IF NOT EXISTS accounts (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20)     NOT NULL,
    account_type   VARCHAR(20)     NOT NULL,
    balance        DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    customer_id    BIGINT          NOT NULL,
    is_active      TINYINT(1)      NOT NULL DEFAULT 1,
    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_account_number (account_number),
    INDEX idx_account_customer_id (customer_id),
    INDEX idx_account_type (account_type),
    INDEX idx_is_active (is_active),

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

