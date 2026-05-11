package com.example.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDTO {

    private String accountNumber;
    private String accountType;          // SAVINGS, CURRENT, FIXED_DEPOSIT, LOAN
    private BigDecimal balance;
    private Long customerId;

    // ── Status & limits ────────────────────────────────────────────────────
    private String status;               // ACTIVE, FROZEN, CLOSED, DORMANT
    private BigDecimal dailyTxnLimit;
    private BigDecimal usedDailyAmount;
    private BigDecimal minBalance;

    // ── Interest & FD ──────────────────────────────────────────────────────
    private BigDecimal interestRate;
    private LocalDate maturityDate;      // For FIXED_DEPOSIT accounts

    // ── Branch & nominee ───────────────────────────────────────────────────
    private String nomineeName;
    private String ifscCode;
    private String branchCode;

    // ── Audit ──────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
