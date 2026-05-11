package com.example.transactionservice.service;

import com.example.transactionservice.Repo.TransactionRepository;
import com.example.transactionservice.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based fraud detection engine.
 * Evaluates each completed transaction against known fraud patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;

    private static final int    VELOCITY_WINDOW_MINUTES = 10;
    private static final int    VELOCITY_MAX_COUNT      = 5;
    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("500000");  // 5 lakh
    private static final BigDecimal OFF_HOURS_THRESHOLD    = new BigDecimal("10000");   // 10k
    private static final BigDecimal ROUND_AMOUNT_THRESHOLD = new BigDecimal("10000");

    public FraudCheckResult check(Transaction txn) {
        List<String> triggeredRules = new ArrayList<>();

        // Rule 1: Velocity — too many transactions in a short window
        LocalDateTime since = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        long recentCount = transactionRepository.countRecentTransactions(txn.getAccountNumber(), since);
        if (recentCount > VELOCITY_MAX_COUNT) {
            triggeredRules.add(String.format("VELOCITY: %d transactions in the last %d minutes",
                    recentCount, VELOCITY_WINDOW_MINUTES));
        }

        // Rule 2: Large single transaction
        if (txn.getAmount().compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            triggeredRules.add("LARGE_AMOUNT: Single transaction exceeds ₹5,00,000");
        }

        // Rule 3: Off-hours high-value transaction (11 PM – 5 AM)
        LocalTime now = LocalTime.now();
        boolean offHours = now.isAfter(LocalTime.of(23, 0)) || now.isBefore(LocalTime.of(5, 0));
        if (offHours && txn.getAmount().compareTo(OFF_HOURS_THRESHOLD) > 0) {
            triggeredRules.add("OFF_HOURS: High-value transaction during off-hours (11 PM – 5 AM)");
        }

        // Rule 4: Round-amount pattern (potential money-laundering signal)
        if (txn.getAmount().compareTo(ROUND_AMOUNT_THRESHOLD) >= 0
                && txn.getAmount().remainder(ROUND_AMOUNT_THRESHOLD).compareTo(BigDecimal.ZERO) == 0) {
            triggeredRules.add("ROUND_AMOUNT: Amount is exact multiple of ₹10,000");
        }

        boolean flagged = !triggeredRules.isEmpty();
        String severity = computeSeverity(triggeredRules.size());

        if (flagged) {
            log.warn("Fraud flags on account={} txnRef={} rules={}",
                    txn.getAccountNumber(), txn.getReferenceNumber(), triggeredRules);
        }

        return FraudCheckResult.builder()
                .flagged(flagged)
                .triggeredRules(triggeredRules)
                .severity(flagged ? severity : null)
                .build();
    }

    private String computeSeverity(int ruleCount) {
        if (ruleCount >= 3) return "HIGH";
        if (ruleCount == 2) return "MEDIUM";
        return "LOW";
    }

    // ── Inner result DTO ─────────────────────────────────────────────────────
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FraudCheckResult {
        private boolean flagged;
        private List<String> triggeredRules;
        private String severity; // LOW, MEDIUM, HIGH
    }
}

