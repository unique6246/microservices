-- V1: Initial schema for notification_db
-- NotificationService owns the 'notifications' table

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    receiver   VARCHAR(100) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_notification_receiver   (receiver),
    INDEX idx_notification_status     (status),
    INDEX idx_notification_created_at (created_at),

    CONSTRAINT chk_notification_status CHECK (status IN ('SENT', 'FAILED', 'PENDING'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

