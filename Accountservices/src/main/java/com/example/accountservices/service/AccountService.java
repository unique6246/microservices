package com.example.accountservices.service;

import com.example.accountservices.Repo.AccountRepository;
import com.example.accountservices.accountUtils.AccountUtils;
import com.example.accountservices.dto.*;
import com.example.accountservices.entity.Account;
import com.example.accountservices.exception.BusinessRuleException;
import com.example.accountservices.exception.DuplicateResourceException;
import com.example.accountservices.exception.ResourceNotFoundException;
import com.example.accountservices.config.NotificationPublisher;
import com.example.accountservices.feignconfig.CustomerServiceClient;
import com.example.accountservices.service.impli.AccountServiceImpl;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements AccountServiceImpl {

    private final NotificationPublisher  notificationPublisher;
    private final AccountRepository      accountRepository;
    private final CustomerServiceClient  customerServiceClient;
    private final BCryptPasswordEncoder  passwordEncoder = new BCryptPasswordEncoder();

    // Default limits per account type.
    // FIXED_DEPOSIT and LOAN are intentionally 0 — no daily debit transactions are permitted
    // on these account types (enforced here and in TransactionService).
    // The DB constraint chk_daily_limit_non_negative (>= 0) allows this value (V3 migration).
    private static final Map<String, BigDecimal> DEFAULT_DAILY_LIMITS = Map.of(
            "SAVINGS",       new BigDecimal("100000"),
            "CURRENT",       new BigDecimal("1000000"),
            "FIXED_DEPOSIT", BigDecimal.ZERO,
            "LOAN",          BigDecimal.ZERO
    );
    private static final Map<String, BigDecimal> DEFAULT_MIN_BALANCES = Map.of(
            "SAVINGS",       new BigDecimal("500"),
            "CURRENT",       BigDecimal.ZERO,
            "FIXED_DEPOSIT", BigDecimal.ZERO,
            "LOAN",          BigDecimal.ZERO
    );
    private static final Map<String, BigDecimal> DEFAULT_INTEREST_RATES = Map.of(
            "SAVINGS",       new BigDecimal("3.50"),
            "CURRENT",       BigDecimal.ZERO,
            "FIXED_DEPOSIT", new BigDecimal("7.00"),
            "LOAN",          BigDecimal.ZERO
    );

    @Value("${account.exchange.name}")
    private String EXCHANGE_NAME;

    @Value("${account.routing.json.key}")
    private String JSON_ROUTING_KEY;

    // ── Queries ─────────────────────────────────────────────────────────────

    public Page<AccountDTO> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::convertToDTO);
    }

    public AccountDTO getAccountById(Long id) {
        return accountRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
    }

    @Cacheable(value = "accounts", key = "#customerId")
    public AccountDTO getAccountByCustomerId(Long customerId) {
        if (!accountRepository.existsAccountByCustomerId(customerId)) {
            throw new ResourceNotFoundException("Account", "customerId", customerId);
        }
        return convertToDTO(accountRepository.findAccountByCustomerId(customerId));
    }

    @Cacheable(value = "accountByNo", key = "#accountNumber")
    public AccountDTO getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) {
            throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        }
        return convertToDTO(account);
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    @CircuitBreaker(name = "customerService", fallbackMethod = "createAccountFallback")
    @Retry(name = "customerService")
    public BankDto createAccount(AccountDTO accountDTO) {
        if (accountRepository.existsAccountByCustomerId(accountDTO.getCustomerId())) {
            throw new DuplicateResourceException(
                    "Account already exists for customerId: " + accountDTO.getCustomerId());
        }

        String type = accountDTO.getAccountType();
        Account newAccount = accountRepository.save(Account.builder()
                .accountNumber(AccountUtils.generateAccountNumber())
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .customerId(accountDTO.getCustomerId())
                .dailyTxnLimit(DEFAULT_DAILY_LIMITS.getOrDefault(type, new BigDecimal("100000")))
                .minBalance(DEFAULT_MIN_BALANCES.getOrDefault(type, BigDecimal.ZERO))
                .interestRate(DEFAULT_INTEREST_RATES.getOrDefault(type, BigDecimal.ZERO))
                .nomineeName(accountDTO.getNomineeName())
                .maturityDate(accountDTO.getMaturityDate())
                .build());

        CustomerDTO customer = customerServiceClient.getCustomerById(newAccount.getCustomerId());
        publishNotification(customer.getEmail(), "Welcome to Our Bank",
                buildAccountMessage(newAccount, AccountUtils.ACCOUNT_CREATION_CODE, AccountUtils.ACCOUNT_CREATION_MESSAGE));

        log.info("Account created: accountNumber={}, type={}, customerId={}",
                newAccount.getAccountNumber(), type, newAccount.getCustomerId());
        return buildResponse(AccountUtils.ACCOUNT_CREATION_CODE, AccountUtils.ACCOUNT_CREATION_MESSAGE,
                newAccount.getAccountNumber(), newAccount.getBalance(),
                customer.getFirstName() + " " + customer.getLastName());
    }

    @Transactional
    @CacheEvict(value = {"accounts", "accountByNo"}, allEntries = true)
    public BankDto deleteAccount(String accountNumber) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot close account with non-zero balance. Please withdraw ₹" + account.getBalance() + " first.");
        }
        accountRepository.deleteAccountByAccountNumber(accountNumber);

        try {
            CustomerDTO customer = customerServiceClient.getCustomerById(account.getCustomerId());
            publishNotification(customer.getEmail(), "Account Closed",
                    buildAccountMessage(account, AccountUtils.ACCOUNT_DELETION_CODE, AccountUtils.ACCOUNT_DELETION_MESSAGE));
        } catch (Exception e) {
            log.warn("Could not send deletion notification for account={}: {}", accountNumber, e.getMessage());
        }

        log.info("Account closed: accountNumber={}", accountNumber);
        return buildResponse(AccountUtils.ACCOUNT_DELETION_CODE, AccountUtils.ACCOUNT_DELETION_MESSAGE, null, null, null);
    }

    @Transactional
    @CacheEvict(value = {"accountByNo"}, key = "#accountNumber")
    public String saveAccount(String accountNumber, BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Balance cannot be negative");
        }
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        account.setBalance(balance);
        accountRepository.save(account);
        log.info("Balance updated: accountNumber={}, newBalance={}", accountNumber, balance);
        return "Balance updated successfully";
    }

    // ── Account Status Management ────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"accounts", "accountByNo"}, allEntries = true)
    public AccountDTO freezeAccount(String accountNumber, String reason) {
        Account account = getActiveAccountOrThrow(accountNumber);
        account.setStatus("FROZEN");
        account.setIsActive(false);
        accountRepository.save(account);
        log.info("Account frozen: accountNumber={}, reason={}", accountNumber, reason);
        return convertToDTO(account);
    }

    @Transactional
    @CacheEvict(value = {"accounts", "accountByNo"}, allEntries = true)
    public AccountDTO unfreezeAccount(String accountNumber) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        if (!"FROZEN".equals(account.getStatus())) {
            throw new BusinessRuleException("Account is not frozen. Current status: " + account.getStatus());
        }
        account.setStatus("ACTIVE");
        account.setIsActive(true);
        accountRepository.save(account);
        log.info("Account unfrozen: accountNumber={}", accountNumber);
        return convertToDTO(account);
    }

    // ── Daily Limit Management ───────────────────────────────────────────────

    @Transactional
    public AccountDTO updateDailyLimit(String accountNumber, BigDecimal newLimit) {
        if (newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Daily limit must be positive");
        }
        Account account = getActiveAccountOrThrow(accountNumber);
        account.setDailyTxnLimit(newLimit);
        accountRepository.save(account);
        log.info("Daily limit updated: accountNumber={}, newLimit={}", accountNumber, newLimit);
        return convertToDTO(account);
    }

    // ── PIN Management ───────────────────────────────────────────────────────

    @Transactional
    public void setPin(String accountNumber, String rawPin) {
        if (rawPin == null || !rawPin.matches("\\d{4}")) {
            throw new BusinessRuleException("PIN must be exactly 4 digits");
        }
        Account account = getActiveAccountOrThrow(accountNumber);
        account.setPinHash(passwordEncoder.encode(rawPin));
        accountRepository.save(account);
        log.info("PIN set for accountNumber={}", accountNumber);
    }

    public boolean verifyPin(String accountNumber, String rawPin) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        if (account.getPinHash() == null) {
            throw new BusinessRuleException("No PIN set for this account. Please set a PIN first.");
        }
        boolean valid = passwordEncoder.matches(rawPin, account.getPinHash());
        if (!valid) throw new BusinessRuleException("Invalid PIN");
        return true;
    }

    // ── Scheduled: reset daily usage at midnight ─────────────────────────────

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void resetDailyUsage() {
        int count = accountRepository.resetDailyUsage(LocalDate.now());
        log.info("Daily usage reset for {} accounts", count);
    }

    // ── Scheduled: post daily interest at midnight ────────────────────────────

    @Scheduled(cron = "0 5 0 * * ?")  // 00:05 daily
    @Transactional
    public void postDailyInterest() {
        accountRepository.findAllByAccountTypeAndStatus("SAVINGS", "ACTIVE").forEach(account -> {
            if (account.getInterestRate().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyInterest = account.getBalance()
                        .multiply(account.getInterestRate())
                        .divide(new BigDecimal("36500"), 4, RoundingMode.HALF_UP);
                if (dailyInterest.compareTo(BigDecimal.ZERO) > 0) {
                    account.setBalance(account.getBalance().add(dailyInterest));
                    accountRepository.save(account);
                    log.debug("Interest posted: account={}, amount={}", account.getAccountNumber(), dailyInterest);
                }
            }
        });
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────

    public BankDto createAccountFallback(AccountDTO accountDTO, Exception ex) {
        log.error("CustomerService circuit breaker triggered during account creation: {}", ex.getMessage());
        throw new BusinessRuleException("Customer service is currently unavailable. Please try again later.");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private Account getActiveAccountOrThrow(String accountNumber) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        return account;
    }

    private void publishNotification(String receiver, String subject, String body) {
        notificationPublisher.publish(EXCHANGE_NAME, JSON_ROUTING_KEY, receiver, subject, body);
    }

    private String buildAccountMessage(Account account, String code, String message) {
        return String.format("Response Code: %s%nResponse Message: %s%nAccount Number: %s%nBalance: %s",
                code, message, account.getAccountNumber(), account.getBalance());
    }

    private BankDto buildResponse(String code, String message, String accountNumber,
                                   BigDecimal balance, String accountName) {
        return BankDto.builder()
                .responseCode(code)
                .responseMessage(message)
                .accountInfo(AccountInfo.builder()
                        .accountNumber(accountNumber)
                        .accountBalance(balance)
                        .accountName(accountName)
                        .build())
                .build();
    }

    private AccountDTO convertToDTO(Account account) {
        return AccountDTO.builder()
                .accountType(account.getAccountType())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .customerId(account.getCustomerId())
                .status(account.getStatus())
                .dailyTxnLimit(account.getDailyTxnLimit())
                .usedDailyAmount(account.getUsedDailyAmount())
                .minBalance(account.getMinBalance())
                .interestRate(account.getInterestRate())
                .maturityDate(account.getMaturityDate())
                .nomineeName(account.getNomineeName())
                .ifscCode(account.getIfscCode())
                .branchCode(account.getBranchCode())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
