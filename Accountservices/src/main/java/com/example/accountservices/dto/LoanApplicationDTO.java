package com.example.accountservices.dto;

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
@Schema(description = "Loan application request")
public class LoanApplicationDTO {

    @NotNull(message = "Customer ID is required")
    @Schema(example = "1")
    private Long customerId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "10000.00", message = "Minimum loan amount is 10,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount is 1 crore")
    @Schema(example = "500000.00")
    private BigDecimal principalAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months (30 years)")
    @Schema(example = "60")
    private Integer tenureMonths;

    @NotBlank(message = "Loan type is required")
    @Pattern(regexp = "^(PERSONAL|HOME|AUTO|EDUCATION)$",
            message = "Loan type must be PERSONAL, HOME, AUTO, or EDUCATION")
    @Schema(example = "PERSONAL")
    private String loanType;

    @NotBlank(message = "Linked savings account is required")
    @Schema(example = "ACC-1234567890")
    private String linkedSavingsAccount;

    @Schema(example = "John Doe")
    private String nomineeName;
}

