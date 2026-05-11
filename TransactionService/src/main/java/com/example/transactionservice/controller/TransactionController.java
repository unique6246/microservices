package com.example.transactionservice.controller;

import com.example.transactionservice.dto.*;
import com.example.transactionservice.service.TransactionServiceImpl;
import com.example.transactionservice.service.impli.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Credit, debit, transfer, NEFT, RTGS, UPI, statement")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionServiceImpl transactionServiceImpl;

    // ── Queries ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all transactions (paginated)")
    public ResponseEntity<Page<TransactionDTO>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get transactions by account number")
    public ResponseEntity<List<TransactionDTO>> getByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountNumber(accountNumber));
    }

    @GetMapping("/type/{transactionType}")
    @Operation(summary = "Get transactions by type (CREDIT or DEBIT)")
    public ResponseEntity<List<TransactionDTO>> getByType(@PathVariable String transactionType) {
        return ResponseEntity.ok(transactionService.getTransactionsByTransactionType(transactionType));
    }

    @GetMapping("/ref/{referenceNumber}")
    @Operation(summary = "Get a transaction by its reference number")
    public ResponseEntity<TransactionDTO> getByReferenceNumber(@PathVariable String referenceNumber) {
        return ResponseEntity.ok(transactionServiceImpl.getByReferenceNumber(referenceNumber));
    }

    @GetMapping("/fraud/flagged")
    @Operation(summary = "List fraud-flagged transactions (admin)")
    public ResponseEntity<Page<TransactionDTO>> getFlaggedTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionServiceImpl.getFlaggedTransactions(pageable));
    }

    @GetMapping("/account/{accountNumber}/statement")
    @Operation(summary = "Get statement transactions for a date range")
    public ResponseEntity<List<TransactionDTO>> getStatement(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(transactionServiceImpl.getStatement(accountNumber, from, to));
    }

    @GetMapping("/account/{accountNumber}/summary")
    @Operation(summary = "Get credit/debit summary for an account over a date range")
    public ResponseEntity<StatementSummaryDTO> getStatementSummary(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(transactionServiceImpl.getStatementSummary(accountNumber, from, to));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @PostMapping("/credit")
    @Operation(summary = "Credit amount to an account")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Credit processed") })
    public ResponseEntity<BankDto> creditTransaction(@Valid @RequestBody TransactionDTO dto) {
        log.info("Credit request: account={}, amount={}", dto.getAccountNumber(), dto.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.creditTransaction(dto));
    }

    @PostMapping("/debit")
    @Operation(summary = "Debit amount from an account")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Debit processed") })
    public ResponseEntity<BankDto> debitTransaction(@Valid @RequestBody TransactionDTO dto) {
        log.info("Debit request: account={}, amount={}", dto.getAccountNumber(), dto.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.debitTransaction(dto));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Internal transfer between two accounts")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Transfer completed") })
    public ResponseEntity<BankDto> transferBetweenUsers(@Valid @RequestBody TransferDTO dto) {
        log.info("Transfer request: from={}, to={}, amount={}", dto.getFromAccount(), dto.getToAccount(), dto.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transferBetweenUsers(dto));
    }

    @PostMapping("/neft")
    @Operation(summary = "NEFT / RTGS / IMPS transfer to an external bank account")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Transfer initiated") })
    public ResponseEntity<BankDto> neftRtgsTransfer(@Valid @RequestBody NeftRtgsDTO dto) {
        log.info("NEFT/RTGS request: from={}, channel={}, amount={}", dto.getFromAccount(), dto.getChannel(), dto.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionServiceImpl.neftRtgsTransfer(dto));
    }

    @PostMapping("/upi")
    @Operation(summary = "UPI payment to a VPA (Virtual Payment Address)")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "UPI payment sent") })
    public ResponseEntity<BankDto> upiTransfer(@Valid @RequestBody UpiDTO dto) {
        log.info("UPI request: from={}, upiId={}, amount={}", dto.getFromAccount(), dto.getUpiId(), dto.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionServiceImpl.upiTransfer(dto));
    }
}
