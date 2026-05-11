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

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Bank account lifecycle management")
public class AccountController {

    private final AccountService accountService;

    // ── Queries ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all accounts (paginated)")
    public ResponseEntity<Page<AccountDTO>> getAllAccounts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Fetching all accounts, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(accountService.getAllAccounts(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
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

    // ── Commands ─────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Open a new bank account (SAVINGS, CURRENT, FIXED_DEPOSIT, LOAN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Account already exists")
    })
    public ResponseEntity<BankDto> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
        log.info("Creating account for customerId={}, type={}", accountDTO.getCustomerId(), accountDTO.getAccountType());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(accountDTO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update account balance")
    public ResponseEntity<String> saveAccount(@Valid @RequestBody AccountDTO accountDTO) {
        log.info("Updating balance for account={}", accountDTO.getAccountNumber());
        return ResponseEntity.ok(accountService.saveAccount(accountDTO.getAccountNumber(), accountDTO.getBalance()));
    }

    @DeleteMapping("/{accountNumber}")
    @Operation(summary = "Close a bank account (balance must be zero)")
    public ResponseEntity<BankDto> deleteAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.deleteAccount(accountNumber));
    }

    // ── Account Status ────────────────────────────────────────────────────────

    @PatchMapping("/{accountNumber}/freeze")
    @Operation(summary = "Freeze an account (blocks all transactions)")
    public ResponseEntity<AccountDTO> freezeAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "Admin freeze") String reason) {
        log.info("Freezing account={}, reason={}", accountNumber, reason);
        return ResponseEntity.ok(accountService.freezeAccount(accountNumber, reason));
    }

    @PatchMapping("/{accountNumber}/unfreeze")
    @Operation(summary = "Unfreeze a frozen account")
    public ResponseEntity<AccountDTO> unfreezeAccount(@PathVariable String accountNumber) {
        log.info("Unfreezing account={}", accountNumber);
        return ResponseEntity.ok(accountService.unfreezeAccount(accountNumber));
    }

    // ── Daily Limit ───────────────────────────────────────────────────────────

    @PatchMapping("/{accountNumber}/limits")
    @Operation(summary = "Update daily transaction limit for an account")
    public ResponseEntity<AccountDTO> updateDailyLimit(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal dailyLimit) {
        log.info("Updating daily limit: account={}, limit={}", accountNumber, dailyLimit);
        return ResponseEntity.ok(accountService.updateDailyLimit(accountNumber, dailyLimit));
    }

    // ── PIN Management ────────────────────────────────────────────────────────

    @PostMapping("/{accountNumber}/pin/set")
    @Operation(summary = "Set a 4-digit TPIN for the account")
    public ResponseEntity<Map<String, String>> setPin(
            @PathVariable String accountNumber,
            @RequestParam String pin) {
        log.info("Setting PIN for account={}", accountNumber);
        accountService.setPin(accountNumber, pin);
        return ResponseEntity.ok(Map.of("message", "PIN set successfully"));
    }

    @PostMapping("/{accountNumber}/pin/verify")
    @Operation(summary = "Verify the account TPIN")
    public ResponseEntity<Map<String, Object>> verifyPin(
            @PathVariable String accountNumber,
            @RequestParam String pin) {
        boolean valid = accountService.verifyPin(accountNumber, pin);
        return ResponseEntity.ok(Map.of("valid", valid, "message", "PIN verified successfully"));
    }
}
