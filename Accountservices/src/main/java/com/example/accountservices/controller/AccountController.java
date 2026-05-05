package com.example.accountservices.controller;

import com.example.accountservices.dto.AccountDTO;
import com.example.accountservices.dto.BankDto;
import com.example.accountservices.service.AccountService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Bank account lifecycle management")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "List all accounts", description = "Returns a paginated list of all accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<AccountDTO>> getAllAccounts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Fetching all accounts, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(accountService.getAllAccounts(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<AccountDTO> getAccountById(
            @Parameter(description = "Account ID") @PathVariable Long id) {
        log.info("Fetching account id={}", id);
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get account by customer ID")
    public ResponseEntity<AccountDTO> getAccountByCustomerId(
            @Parameter(description = "Customer ID") @PathVariable Long customerId) {
        log.info("Fetching account for customerId={}", customerId);
        return ResponseEntity.ok(accountService.getAccountByCustomerId(customerId));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<AccountDTO> getAccountByAccountNumber(
            @Parameter(description = "Account number") @PathVariable String accountNumber) {
        log.info("Fetching account number={}", accountNumber);
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @PostMapping
    @Operation(summary = "Open a new bank account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Account already exists")
    })
    public ResponseEntity<BankDto> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
        log.info("Creating account for customerId={}", accountDTO.getCustomerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(accountDTO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update account balance")
    @ApiResponse(responseCode = "200", description = "Balance updated")
    public ResponseEntity<String> saveAccount(@Valid @RequestBody AccountDTO accountDTO) {
        log.info("Updating balance for account={}", accountDTO.getAccountNumber());
        return ResponseEntity.ok(accountService.saveAccount(accountDTO.getAccountNumber(), accountDTO.getBalance()));
    }

    @DeleteMapping("/{accountNumber}")
    @Operation(summary = "Close a bank account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account closed"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<BankDto> deleteAccount(
            @Parameter(description = "Account number to close") @PathVariable String accountNumber) {
        log.info("Closing account number={}", accountNumber);
        return ResponseEntity.ok(accountService.deleteAccount(accountNumber));
    }
}
