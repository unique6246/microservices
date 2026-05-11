package com.example.accountservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_number", columnNames = "account_number"),
        indexes = {
                @Index(name = "idx_account_customer_id", columnList = "customer_id"),
                @Index(name = "idx_account_number", columnList = "account_number"),
                @Index(name = "idx_account_status", columnList = "status")
        })
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType; // SAVINGS, CURRENT, FIXED_DEPOSIT, LOAN

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ── Real-world banking fields ───────────────────────────────────────────

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, FROZEN, CLOSED, DORMANT

    @Column(name = "daily_txn_limit", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTxnLimit = new BigDecimal("100000.00");

    @Column(name = "used_daily_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal usedDailyAmount = BigDecimal.ZERO;

    @Column(name = "min_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minBalance = BigDecimal.ZERO;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "maturity_date")
    private LocalDate maturityDate; // For FIXED_DEPOSIT accounts

    @Column(name = "nominee_name", length = 100)
    private String nomineeName;

    @Column(name = "pin_hash", length = 60)
    private String pinHash; // BCrypt-hashed 4-digit TPIN

    @Column(name = "branch_code", nullable = false, length = 10)
    @Builder.Default
    private String branchCode = "MAIN";

    @Column(name = "ifsc_code", nullable = false, length = 11)
    @Builder.Default
    private String ifscCode = "BANK0000001";

    @Column(name = "daily_reset_date")
    private LocalDate dailyResetDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
