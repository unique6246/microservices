-- V2: Real-world extensions for transaction_db
ALTER TABLE transactions DROP CHECK chk_txn_type;
ALTER TABLE transactions
    ADD COLUMN channel             VARCHAR(20)   NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN status              VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN reference_number    VARCHAR(30)   NULL,
    ADD COLUMN beneficiary_account VARCHAR(20)   NULL,
    ADD COLUMN beneficiary_name    VARCHAR(100)  NULL,
    ADD COLUMN beneficiary_ifsc    VARCHAR(11)   NULL,
    ADD COLUMN bank_name           VARCHAR(100)  NULL,
    ADD COLUMN processing_fee      DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    ADD COLUMN upi_id              VARCHAR(50)   NULL,
    ADD COLUMN remarks             VARCHAR(255)  NULL,
    ADD COLUMN fraud_flag          TINYINT(1)    NOT NULL DEFAULT 0,
    ADD COLUMN fraud_reason        VARCHAR(500)  NULL,
    ADD COLUMN fraud_severity      VARCHAR(10)   NULL;
ALTER TABLE transactions ADD UNIQUE KEY uk_reference_number (reference_number);
ALTER TABLE transactions
    ADD CONSTRAINT chk_txn_type_v2  CHECK (transaction_type IN ('CREDIT','DEBIT')),
    ADD CONSTRAINT chk_txn_channel  CHECK (channel IN ('INTERNAL','NEFT','RTGS','IMPS','UPI')),
    ADD CONSTRAINT chk_txn_status   CHECK (status IN ('INITIATED','PROCESSING','COMPLETED','FAILED','REVERSED'));
ALTER TABLE transactions
    ADD INDEX idx_txn_reference  (reference_number),
    ADD INDEX idx_txn_channel    (channel),
    ADD INDEX idx_txn_status     (status),
    ADD INDEX idx_txn_fraud_flag (fraud_flag);
