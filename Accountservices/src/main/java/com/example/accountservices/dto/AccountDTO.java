package com.example.accountservices.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Account data transfer object")
public class AccountDTO {

    @Schema(description = "Account number (system-generated)", accessMode = Schema.AccessMode.READ_ONLY)
    private String accountNumber;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "^(SAVINGS|CURRENT|FIXED_DEPOSIT|LOAN)$",
            message = "Account type must be SAVINGS, CURRENT, FIXED_DEPOSIT, or LOAN")
    @Schema(description = "Type of account", example = "SAVINGS")
    private String accountType;

    @PositiveOrZero(message = "Balance cannot be negative")
    @Schema(description = "Current account balance", example = "1000.00")
    private BigDecimal balance;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Owner customer ID", example = "1")
    private Long customerId;

    // ── Real-world fields ──────────────────────────────────────────────────

    @Schema(description = "Account status", example = "ACTIVE", accessMode = Schema.AccessMode.READ_ONLY)
    private String status;

    @Schema(description = "Maximum daily transaction limit", example = "100000.00")
    private BigDecimal dailyTxnLimit;

    @Schema(description = "Amount used today for debits", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal usedDailyAmount;

    @Schema(description = "Minimum balance requirement", example = "500.00")
    private BigDecimal minBalance;

    @Schema(description = "Annual interest rate (%)", example = "3.50")
    private BigDecimal interestRate;

    @Schema(description = "Maturity date for FD accounts")
    private LocalDate maturityDate;

    @Schema(description = "Nominee full name")
    private String nomineeName;

    @Schema(description = "IFSC code of the branch", example = "BANK0000001")
    private String ifscCode;

    @Schema(description = "Branch code", example = "MAIN")
    private String branchCode;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}
