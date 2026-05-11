package com.example.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NEFT / RTGS transfer request")
public class NeftRtgsDTO {

    @NotBlank(message = "Source account is required")
    @Schema(example = "ACC-1234567890")
    private String fromAccount;

    @NotBlank(message = "Beneficiary account number is required")
    @Schema(example = "9876543210")
    private String beneficiaryAccountNumber;

    @NotBlank(message = "Beneficiary name is required")
    @Schema(example = "John Doe")
    private String beneficiaryName;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    @Schema(example = "HDFC0001234")
    private String ifscCode;

    @NotBlank(message = "Bank name is required")
    @Schema(example = "HDFC Bank")
    private String bankName;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(example = "5000.00")
    private BigDecimal amount;

    @NotBlank(message = "Channel is required (NEFT or RTGS)")
    @Pattern(regexp = "^(NEFT|RTGS|IMPS)$", message = "Channel must be NEFT, RTGS, or IMPS")
    @Schema(example = "NEFT")
    private String channel;

    @Schema(example = "Payment for services rendered")
    private String remarks;

    @Schema(description = "Client-provided idempotency key (UUID)")
    private String idempotencyKey;
}

