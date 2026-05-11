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
@Table(name = "otp_records",
        indexes = {
                @Index(name = "idx_otp_customer_id", columnList = "customer_id"),
                @Index(name = "idx_otp_expires",     columnList = "expires_at")
        })
public class OtpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "otp_hash", nullable = false, length = 60)
    private String otpHash; // BCrypt hash

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose; // TRANSACTION_AUTH, TPIN_RESET, ACCOUNT_UNFREEZE

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

