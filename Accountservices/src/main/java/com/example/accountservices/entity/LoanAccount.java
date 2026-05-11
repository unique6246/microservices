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
@Table(name = "loan_accounts",
        indexes = {
                @Index(name = "idx_loan_customer_id", columnList = "customer_id"),
                @Index(name = "idx_loan_status",      columnList = "status"),
                @Index(name = "idx_loan_account_id",  columnList = "account_id")
        })
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId; // FK to accounts.id (the LOAN-type account)

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "loan_type", nullable = false, length = 20)
    private String loanType; // PERSONAL, HOME, AUTO, EDUCATION

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "disbursed_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal disbursedAmount = BigDecimal.ZERO;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal emiAmount;

    @Column(name = "first_emi_date", nullable = false)
    private LocalDate firstEmiDate;

    @Column(name = "next_emi_date", nullable = false)
    private LocalDate nextEmiDate;

    @Column(name = "emis_paid", nullable = false)
    @Builder.Default
    private Integer emisPaid = 0;

    @Column(name = "total_emis", nullable = false)
    private Integer totalEmis;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CLOSED, OVERDUE, NPA

    @Column(name = "linked_savings_acc", nullable = false, length = 20)
    private String linkedSavingsAcc; // Disbursement + repayment account

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

