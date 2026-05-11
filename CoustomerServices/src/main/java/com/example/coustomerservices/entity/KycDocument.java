package com.example.coustomerservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "kyc_documents",
        indexes = {
                @Index(name = "idx_kyc_customer_id", columnList = "customer_id"),
                @Index(name = "idx_kyc_status",      columnList = "status")
        })
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType; // AADHAAR, PAN, PASSPORT, DRIVING_LICENSE

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "document_url", length = 255)
    private String documentUrl;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UPLOADED"; // UPLOADED, VERIFIED, REJECTED

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}

