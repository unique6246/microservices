package com.example.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Account statement request")
public class StatementRequestDTO {

    @NotBlank(message = "Account number is required")
    @Schema(example = "ACC-1234567890")
    private String accountNumber;

    @NotNull(message = "From date is required")
    @Schema(example = "2026-01-01")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    @Schema(example = "2026-03-31")
    private LocalDate toDate;

    @Schema(description = "Statement format: PDF or CSV", example = "PDF")
    private String format; // PDF or CSV, defaults to PDF
}

