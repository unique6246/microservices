package com.example.accountservices.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Account data transfer object")
public class AccountDTO {

    @Schema(description = "Account number (system-generated)", example = "ACC-1234567890", accessMode = Schema.AccessMode.READ_ONLY)
    private String accountNumber;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "^(SAVINGS|CURRENT|FIXED_DEPOSIT)$",
            message = "Account type must be SAVINGS, CURRENT, or FIXED_DEPOSIT")
    @Schema(description = "Type of account", example = "SAVINGS")
    private String accountType;

    @PositiveOrZero(message = "Balance cannot be negative")
    @Schema(description = "Current account balance", example = "1000.00")
    private BigDecimal balance;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Owner customer ID", example = "1")
    private Long customerId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}
