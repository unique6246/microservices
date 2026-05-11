-- V2: Real-world extensions for customer_db
ALTER TABLE customers DROP CHECK chk_gender;
ALTER TABLE customers
    ADD COLUMN date_of_birth    DATE         NULL,
    ADD COLUMN pan_number       VARCHAR(10)  NULL UNIQUE COMMENT 'PAN card number',
    ADD COLUMN aadhaar_last4    VARCHAR(4)   NULL COMMENT 'Last 4 digits of Aadhaar',
    ADD COLUMN kyc_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN kyc_verified_at  DATETIME(6)  NULL,
    ADD COLUMN profile_photo    VARCHAR(255) NULL;
ALTER TABLE customers
    ADD CONSTRAINT chk_gender_v2   CHECK (gender IN ('MALE','FEMALE','OTHER')),
    ADD CONSTRAINT chk_kyc_status  CHECK (kyc_status IN ('PENDING','UNDER_REVIEW','VERIFIED','REJECTED'));
ALTER TABLE customers ADD INDEX idx_kyc_status (kyc_status);
-- KYC documents table
CREATE TABLE IF NOT EXISTS kyc_documents (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT        NOT NULL,
    document_type   VARCHAR(30)   NOT NULL COMMENT 'AADHAAR, PAN, PASSPORT, DRIVING_LICENSE',
    document_number VARCHAR(50)   NOT NULL,
    document_url    VARCHAR(255)  NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'UPLOADED',
    rejection_reason VARCHAR(255) NULL,
    uploaded_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at     DATETIME(6)   NULL,
    PRIMARY KEY (id),
    INDEX idx_kyc_customer_id (customer_id),
    INDEX idx_kyc_status      (status),
    CONSTRAINT chk_doc_type   CHECK (document_type IN ('AADHAAR','PAN','PASSPORT','DRIVING_LICENSE')),
    CONSTRAINT chk_doc_status CHECK (status IN ('UPLOADED','VERIFIED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- OTP records table
CREATE TABLE IF NOT EXISTS otp_records (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id   BIGINT       NOT NULL,
    otp_hash      VARCHAR(60)  NOT NULL COMMENT 'BCrypt hash',
    purpose       VARCHAR(30)  NOT NULL COMMENT 'TRANSACTION_AUTH, TPIN_RESET, ACCOUNT_UNFREEZE',
    expires_at    DATETIME(6)  NOT NULL,
    used          TINYINT(1)   NOT NULL DEFAULT 0,
    attempt_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_otp_customer_id (customer_id),
    INDEX idx_otp_purpose     (purpose),
    INDEX idx_otp_expires     (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
