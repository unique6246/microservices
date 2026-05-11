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
@Schema(description = "UPI payment request")
public class UpiDTO {

    @NotBlank(message = "Source account is required")
    @Schema(example = "ACC-1234567890")
    private String fromAccount;

    @NotBlank(message = "UPI ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+$", message = "Invalid UPI ID format")
    @Schema(example = "john@upi")
    private String upiId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(example = "250.00")
    private BigDecimal amount;

    @Schema(example = "Splitting dinner bill")
    private String remarks;

    @Schema(description = "Client-provided idempotency key (UUID)")
    private String idempotencyKey;
}

