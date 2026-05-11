package com.example.coustomerservices.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpVerifyDTO {

    @NotNull(message = "Customer ID is required")
    @Schema(example = "1")
    private Long customerId;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    @Schema(example = "123456")
    private String otp;

    @NotBlank(message = "Purpose is required")
    @Schema(example = "TRANSACTION_AUTH")
    private String purpose;
}

