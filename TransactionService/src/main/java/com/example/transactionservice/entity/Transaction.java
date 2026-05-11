package com.example.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_txn_account_number", columnList = "account_number"),
                @Index(name = "idx_txn_type",           columnList = "transaction_type"),
                @Index(name = "idx_txn_created_at",     columnList = "created_at"),
                @Index(name = "idx_txn_reference",      columnList = "reference_number"),
                @Index(name = "idx_txn_channel",        columnList = "channel"),
                @Index(name = "idx_txn_fraud_flag",     columnList = "fraud_flag")
        })
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 500)
    private String message;

    @Column(name = "transaction_type", nullable = false, length = 10)
    private String transactionType; // CREDIT or DEBIT

    /** Idempotency key prevents duplicate transactions on retried requests */
    @Column(name = "idempotency_key", unique = true, length = 36)
    private String idempotencyKey;

    // ── Real-world banking fields ────────────────────────────────────────────

    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private String channel = "INTERNAL"; // INTERNAL, NEFT, RTGS, IMPS, UPI

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED"; // INITIATED, PROCESSING, COMPLETED, FAILED, REVERSED

    @Column(name = "reference_number", unique = true, length = 30)
    private String referenceNumber; // TXN-20260511-ABC123

    @Column(name = "beneficiary_account", length = 20)
    private String beneficiaryAccount;

    @Column(name = "beneficiary_name", length = 100)
    private String beneficiaryName;

    @Column(name = "beneficiary_ifsc", length = 11)
    private String beneficiaryIfsc;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "processing_fee", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "upi_id", length = 50)
    private String upiId;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "fraud_flag", nullable = false)
    @Builder.Default
    private Boolean fraudFlag = false;

    @Column(name = "fraud_reason", length = 500)
    private String fraudReason;

    @Column(name = "fraud_severity", length = 10)
    private String fraudSeverity; // LOW, MEDIUM, HIGH

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
