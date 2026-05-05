package com.example.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

/**
 * Fallback controller invoked when a downstream service's circuit breaker opens.
 * Returns RFC 7807 ProblemDetail responses.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/accounts")
    public ProblemDetail accountsFallback() {
        return buildFallback("AccountService", "account management");
    }

    @RequestMapping("/customers")
    public ProblemDetail customersFallback() {
        return buildFallback("CustomerService", "customer management");
    }

    @RequestMapping("/transactions")
    public ProblemDetail transactionsFallback() {
        return buildFallback("TransactionService", "transaction processing");
    }

    @RequestMapping("/notifications")
    public ProblemDetail notificationsFallback() {
        return buildFallback("NotificationService", "notifications");
    }

    private ProblemDetail buildFallback(String serviceName, String action) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                serviceName + " is temporarily unavailable. Please retry in a few moments.");
        pd.setType(URI.create("https://banking.example.com/errors/service-unavailable"));
        pd.setTitle("Service Unavailable");
        pd.setProperty("service", serviceName);
        pd.setProperty("action", action);
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("retryAfter", "30s");
        return pd;
    }
}

