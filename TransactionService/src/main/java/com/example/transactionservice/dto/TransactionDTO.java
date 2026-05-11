package com.example.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Transaction request/response DTO")
public class TransactionDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Account number is required")
    @Schema(example = "ACC-1234567890")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(example = "500.00")
    private BigDecimal amount;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String transactionType;

    @Schema(description = "Optional idempotency key", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idempotencyKey;

    // ── Real-world fields ─────────────────────────────────────────────────────

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "NEFT")
    private String channel;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "COMPLETED")
    private String status;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "TXN-20260511132000-123456")
    private String referenceNumber;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String beneficiaryAccount;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String beneficiaryName;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "24.00")
    private BigDecimal processingFee;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean fraudFlag;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String remarks;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
}
