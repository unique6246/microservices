package com.example.coustomerservices.service;

import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.Repo.OtpRepository;
import com.example.coustomerservices.entity.Customer;
import com.example.coustomerservices.entity.OtpRecord;
import com.example.coustomerservices.exception.BusinessRuleException;
import com.example.coustomerservices.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository      otpRepository;
    private final CustomerRepository  customerRepository;
    private final RabbitTemplate      rabbitTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom     = new SecureRandom();

    private static final int OTP_LENGTH     = 6;
    private static final int OTP_TTL_MINS   = 5;
    private static final int MAX_ATTEMPTS   = 3;

    @Value("${account.exchange.name}")
    private String EXCHANGE_NAME;

    @Value("${account.routing.json.key}")
    private String ROUTING_KEY;

    // ── Generate OTP ─────────────────────────────────────────────────────────

    @Transactional
    public String generateOtp(Long customerId, String purpose) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Generate random 6-digit OTP
        String rawOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hash   = encoder.encode(rawOtp);

        // Invalidate previous OTPs for same purpose
        otpRepository.findFirstByCustomerIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                customerId, purpose, LocalDateTime.now())
                .ifPresent(old -> { old.setUsed(true); otpRepository.save(old); });

        otpRepository.save(OtpRecord.builder()
                .customerId(customerId)
                .otpHash(hash)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINS))
                .build());

        // Publish OTP notification event
        publishOtpNotification(customer, rawOtp, purpose);

        log.info("OTP generated for customerId={}, purpose={}", customerId, purpose);

        // In production, never return the raw OTP in the response;
        // return it here only for development/testing
        return rawOtp;
    }

    // ── Verify OTP ───────────────────────────────────────────────────────────

    @Transactional
    public boolean verifyOtp(Long customerId, String rawOtp, String purpose) {
        OtpRecord record = otpRepository
                .findFirstByCustomerIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        customerId, purpose, LocalDateTime.now())
                .orElseThrow(() -> new BusinessRuleException("No valid OTP found. Please request a new OTP."));

        record.setAttemptCount(record.getAttemptCount() + 1);

        if (record.getAttemptCount() > MAX_ATTEMPTS) {
            record.setUsed(true);
            otpRepository.save(record);
            throw new BusinessRuleException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        boolean valid = encoder.matches(rawOtp, record.getOtpHash());
        if (valid) {
            record.setUsed(true);
        }
        otpRepository.save(record);

        if (!valid) {
            throw new BusinessRuleException("Invalid OTP. " + (MAX_ATTEMPTS - record.getAttemptCount()) + " attempts remaining.");
        }

        log.info("OTP verified successfully for customerId={}, purpose={}", customerId, purpose);
        return true;
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private void publishOtpNotification(Customer customer, String otp, String purpose) {
        try {
            String subject = "Your OTP for " + purpose.replace("_", " ").toLowerCase();
            String body = String.format(
                    "Dear %s %s,%n%nYour OTP is: %s%n%nThis OTP is valid for %d minutes.%n%nDo not share this OTP with anyone.",
                    customer.getFirstName(), customer.getLastName(), otp, OTP_TTL_MINS);

            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY,
                    Map.of("receiver", customer.getEmail(), "subject", subject, "body", body));
        } catch (Exception e) {
            log.error("Failed to publish OTP notification: {}", e.getMessage());
        }
    }
}

