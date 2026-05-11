-- V2: Real-world banking extensions for accounts_db

-- Drop restrictive check so we can add LOAN type
ALTER TABLE accounts DROP CHECK chk_account_type;

-- Extend accounts table with real-world fields
ALTER TABLE accounts
    ADD COLUMN status            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'ACTIVE, FROZEN, CLOSED, DORMANT',
    ADD COLUMN daily_txn_limit   DECIMAL(19,4)  NOT NULL DEFAULT 100000.0000
        COMMENT 'Max debit amount per calendar day',
    ADD COLUMN used_daily_amount DECIMAL(19,4)  NOT NULL DEFAULT 0.0000
        COMMENT 'Debit amount used today (reset at midnight)',
    ADD COLUMN min_balance       DECIMAL(19,4)  NOT NULL DEFAULT 0.0000
        COMMENT 'Minimum balance constraint',
    ADD COLUMN interest_rate     DECIMAL(5,2)   NOT NULL DEFAULT 0.00
        COMMENT 'Annual interest rate percentage',
    ADD COLUMN maturity_date     DATE           NULL
        COMMENT 'Used by FIXED_DEPOSIT accounts',
    ADD COLUMN nominee_name      VARCHAR(100)   NULL,
    ADD COLUMN pin_hash          VARCHAR(60)    NULL
        COMMENT 'BCrypt-hashed 4-digit TPIN',
    ADD COLUMN branch_code       VARCHAR(10)    NOT NULL DEFAULT 'MAIN',
    ADD COLUMN ifsc_code         VARCHAR(11)    NOT NULL DEFAULT 'BANK0000001',
    ADD COLUMN daily_reset_date  DATE           NULL
        COMMENT 'Date used_daily_amount was last reset';

-- Add new check constraints
ALTER TABLE accounts
    ADD CONSTRAINT chk_account_type_v2
        CHECK (account_type IN ('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT', 'LOAN')),
    ADD CONSTRAINT chk_status
        CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED', 'DORMANT')),
    ADD CONSTRAINT chk_daily_limit_positive CHECK (daily_txn_limit > 0);

-- Performance indexes
ALTER TABLE accounts
    ADD INDEX idx_account_status (status),
    ADD INDEX idx_account_type_status (account_type, status);

-- ── Loans table ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS loan_accounts (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    account_id         BIGINT         NOT NULL COMMENT 'FK to accounts.id',
    customer_id        BIGINT         NOT NULL,
    loan_type          VARCHAR(20)    NOT NULL COMMENT 'PERSONAL, HOME, AUTO, EDUCATION',
    principal_amount   DECIMAL(19,4)  NOT NULL,
    disbursed_amount   DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    interest_rate      DECIMAL(5,2)   NOT NULL,
    tenure_months      INT            NOT NULL,
    emi_amount         DECIMAL(19,4)  NOT NULL,
    first_emi_date     DATE           NOT NULL,
    next_emi_date      DATE           NOT NULL,
    emis_paid          INT            NOT NULL DEFAULT 0,
    total_emis         INT            NOT NULL,
    status             VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'ACTIVE, CLOSED, OVERDUE, NPA',
    linked_savings_acc VARCHAR(20)    NOT NULL COMMENT 'Disbursement + repayment account',
    created_at         DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_loan_customer_id  (customer_id),
    INDEX idx_loan_status       (status),
    INDEX idx_loan_account_id   (account_id),

    CONSTRAINT chk_loan_type   CHECK (loan_type IN ('PERSONAL', 'HOME', 'AUTO', 'EDUCATION')),
    CONSTRAINT chk_loan_status CHECK (status IN ('ACTIVE', 'CLOSED', 'OVERDUE', 'NPA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── EMI payments table ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS emi_payments (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    loan_account_id      BIGINT         NOT NULL,
    emi_number           INT            NOT NULL,
    principal_component  DECIMAL(19,4)  NOT NULL,
    interest_component   DECIMAL(19,4)  NOT NULL,
    penalty_amount       DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    total_paid           DECIMAL(19,4)  NOT NULL,
    due_date             DATE           NOT NULL,
    paid_date            DATETIME(6)    NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING, PAID, OVERDUE, WAIVED',
    created_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_emi_loan_id   (loan_account_id),
    INDEX idx_emi_due_date  (due_date),
    INDEX idx_emi_status    (status),

    CONSTRAINT chk_emi_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'WAIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

