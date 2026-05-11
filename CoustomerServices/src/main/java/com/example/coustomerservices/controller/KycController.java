package com.example.coustomerservices.controller;

import com.example.coustomerservices.dto.KycDocumentDTO;
import com.example.coustomerservices.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Management", description = "Document submission and KYC verification lifecycle")
public class KycController {

    private final KycService kycService;

    @PostMapping("/documents/upload")
    @Operation(summary = "Upload a KYC document for verification")
    public ResponseEntity<KycDocumentDTO> uploadDocument(@Valid @RequestBody KycDocumentDTO dto) {
        log.info("KYC document upload: customerId={}, type={}", dto.getCustomerId(), dto.getDocumentType());
        return ResponseEntity.status(HttpStatus.CREATED).body(kycService.uploadDocument(dto));
    }

    @GetMapping("/{customerId}/status")
    @Operation(summary = "Get KYC status for a customer")
    public ResponseEntity<Map<String, String>> getKycStatus(@PathVariable Long customerId) {
        return ResponseEntity.ok(Map.of(
                "customerId", String.valueOf(customerId),
                "kycStatus", kycService.getKycStatus(customerId)));
    }

    @GetMapping("/{customerId}/documents")
    @Operation(summary = "List all KYC documents for a customer")
    public ResponseEntity<List<KycDocumentDTO>> getDocuments(@PathVariable Long customerId) {
        return ResponseEntity.ok(kycService.getDocuments(customerId));
    }

    @PutMapping("/documents/{docId}/review")
    @Operation(summary = "Admin: approve or reject a KYC document")
    public ResponseEntity<KycDocumentDTO> reviewDocument(
            @PathVariable Long docId,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionReason) {
        log.info("KYC review: docId={}, status={}", docId, status);
        return ResponseEntity.ok(kycService.reviewDocument(docId, status, rejectionReason));
    }
}

