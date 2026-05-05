package com.example.transactionservice.controller;

import com.example.transactionservice.dto.BankDto;
import com.example.transactionservice.dto.TransactionDTO;
import com.example.transactionservice.dto.TransferDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Credit, debit, and transfer operations")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List all transactions (paginated)")
    public ResponseEntity<Page<TransactionDTO>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Fetching all transactions page={}", pageable.getPageNumber());
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get transactions by account number")
    public ResponseEntity<List<TransactionDTO>> getByAccountNumber(
            @Parameter(description = "Account number") @PathVariable String accountNumber) {
        log.info("Fetching transactions for account={}", accountNumber);
        return ResponseEntity.ok(transactionService.getTransactionsByAccountNumber(accountNumber));
    }

    @GetMapping("/type/{transactionType}")
    @Operation(summary = "Get transactions by type (CREDIT or DEBIT)")
    public ResponseEntity<List<TransactionDTO>> getByType(
            @Parameter(description = "Transaction type: CREDIT or DEBIT") @PathVariable String transactionType) {
        log.info("Fetching transactions type={}", transactionType);
        return ResponseEntity.ok(transactionService.getTransactionsByTransactionType(transactionType));
    }

    @PostMapping("/credit")
    @Operation(summary = "Credit amount to an account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credit processed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    public ResponseEntity<BankDto> creditTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        log.info("Credit request: account={}, amount={}", transactionDTO.getAccountNumber(), transactionDTO.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.creditTransaction(transactionDTO));
    }

    @PostMapping("/debit")
    @Operation(summary = "Debit amount from an account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Debit processed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Insufficient funds")
    })
    public ResponseEntity<BankDto> debitTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        log.info("Debit request: account={}, amount={}", transactionDTO.getAccountNumber(), transactionDTO.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.debitTransaction(transactionDTO));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer between two accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer completed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Insufficient funds or invalid transfer")
    })
    public ResponseEntity<BankDto> transferBetweenUsers(@Valid @RequestBody TransferDTO transferDTO) {
        log.info("Transfer request: from={}, to={}, amount={}", transferDTO.getFromAccount(), transferDTO.getToAccount(), transferDTO.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transferBetweenUsers(transferDTO));
    }
}
