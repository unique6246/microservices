-- V1: Initial schema for customer_db
-- CustomerService owns the 'customers' table

CREATE TABLE IF NOT EXISTS customers (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    first_name   VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(50)  NOT NULL,
    gender       VARCHAR(10)  NOT NULL,
    address      VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20)  NOT NULL,
    email        VARCHAR(100) NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_email (email),
    INDEX idx_customer_email (email),
    INDEX idx_customer_phone (phone_number),

    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

