package com.example.coustomerservices.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpRequestDTO {

    @NotNull(message = "Customer ID is required")
    @Schema(example = "1")
    private Long customerId;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(TRANSACTION_AUTH|TPIN_RESET|ACCOUNT_UNFREEZE)$",
            message = "Invalid OTP purpose")
    @Schema(example = "TRANSACTION_AUTH")
    private String purpose;
}

