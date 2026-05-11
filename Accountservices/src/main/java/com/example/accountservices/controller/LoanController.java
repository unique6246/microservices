package com.example.accountservices.controller;

import com.example.accountservices.dto.LoanApplicationDTO;
import com.example.accountservices.entity.EmiPayment;
import com.example.accountservices.entity.LoanAccount;
import com.example.accountservices.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Management", description = "Loan application, EMI payment, and repayment schedule")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for a loan (PERSONAL, HOME, AUTO, EDUCATION)")
    public ResponseEntity<LoanAccount> applyLoan(@Valid @RequestBody LoanApplicationDTO dto) {
        log.info("Loan application: customerId={}, type={}, amount={}", dto.getCustomerId(), dto.getLoanType(), dto.getPrincipalAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.applyLoan(dto));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all loans for a customer")
    public ResponseEntity<List<LoanAccount>> getCustomerLoans(@PathVariable Long customerId) {
        return ResponseEntity.ok(loanService.getCustomerLoans(customerId));
    }

    @GetMapping("/{loanId}/schedule")
    @Operation(summary = "Get full EMI repayment schedule")
    public ResponseEntity<List<EmiPayment>> getLoanSchedule(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getLoanSchedule(loanId));
    }

    @PostMapping("/{loanId}/pay-emi")
    @Operation(summary = "Pay the next EMI for a loan")
    public ResponseEntity<EmiPayment> payEmi(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "0") BigDecimal amount) {
        log.info("EMI payment: loanId={}", loanId);
        return ResponseEntity.ok(loanService.payEmi(loanId, amount));
    }

    @GetMapping("/overdue")
    @Operation(summary = "List all overdue loans (admin)")
    public ResponseEntity<List<LoanAccount>> getOverdueLoans() {
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }
}

