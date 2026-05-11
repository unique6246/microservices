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
public class KycDocumentDTO {

    private Long id;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "^(AADHAAR|PAN|PASSPORT|DRIVING_LICENSE)$",
            message = "Document type must be AADHAAR, PAN, PASSPORT, or DRIVING_LICENSE")
    @Schema(example = "AADHAAR")
    private String documentType;

    @NotBlank(message = "Document number is required")
    @Schema(example = "1234-5678-9012")
    private String documentNumber;

    @Schema(example = "/documents/aadhaar-123.jpg")
    private String documentUrl;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String status;

    private String rejectionReason;
}

