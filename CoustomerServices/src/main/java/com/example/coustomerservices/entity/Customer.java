package com.example.coustomerservices.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_customer_email",  columnList = "email"),
                @Index(name = "idx_customer_phone",  columnList = "phone_number"),
                @Index(name = "idx_customer_kyc",    columnList = "kyc_status")
        })
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // ── KYC fields ──────────────────────────────────────────────────────────

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "pan_number", unique = true, length = 10)
    private String panNumber;

    @Column(name = "aadhaar_last4", length = 4)
    private String aadhaarLast4;

    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private String kycStatus = "PENDING"; // PENDING, UNDER_REVIEW, VERIFIED, REJECTED

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "profile_photo", length = 255)
    private String profilePhoto;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
