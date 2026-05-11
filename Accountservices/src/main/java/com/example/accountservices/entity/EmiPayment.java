package com.example.accountservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "emi_payments",
        indexes = {
                @Index(name = "idx_emi_loan_id",  columnList = "loan_account_id"),
                @Index(name = "idx_emi_due_date", columnList = "due_date"),
                @Index(name = "idx_emi_status",   columnList = "status")
        })
public class EmiPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_account_id", nullable = false)
    private Long loanAccountId;

    @Column(name = "emi_number", nullable = false)
    private Integer emiNumber;

    @Column(name = "principal_component", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestComponent;

    @Column(name = "penalty_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "total_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPaid;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PAID, OVERDUE, WAIVED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

