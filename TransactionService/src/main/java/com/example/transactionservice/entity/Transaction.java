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
                @Index(name = "idx_txn_type", columnList = "transaction_type"),
                @Index(name = "idx_txn_created_at", columnList = "created_at")
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
