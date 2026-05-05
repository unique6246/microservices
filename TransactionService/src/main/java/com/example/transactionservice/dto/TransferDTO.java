package com.example.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Peer-to-peer transfer request")
public class TransferDTO {

    @NotBlank(message = "Source account number is required")
    @Schema(example = "ACC-1111111111")
    private String fromAccount;

    @NotBlank(message = "Destination account number is required")
    @Schema(example = "ACC-2222222222")
    private String toAccount;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "10.00", message = "Minimum transfer amount is 10.00")
    @Schema(example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Optional idempotency key", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idempotencyKey;
}
