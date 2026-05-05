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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements AccountServiceImpl {

    private final NotificationPublisher notificationPublisher;
    private final AccountRepository accountRepository;
    private final CustomerServiceClient customerServiceClient;

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

        Account newAccount = accountRepository.save(Account.builder()
                .accountNumber(AccountUtils.generateAccountNumber())
                .accountType(accountDTO.getAccountType())
                .balance(BigDecimal.ZERO)
                .customerId(accountDTO.getCustomerId())
                .build());

        CustomerDTO customer = customerServiceClient.getCustomerById(newAccount.getCustomerId());
        publishNotification(customer.getEmail(), "Welcome to Our Bank",
                buildAccountMessage(newAccount, AccountUtils.ACCOUNT_CREATION_CODE, AccountUtils.ACCOUNT_CREATION_MESSAGE));

        log.info("Account created: accountNumber={}, customerId={}", newAccount.getAccountNumber(), newAccount.getCustomerId());
        return buildResponse(AccountUtils.ACCOUNT_CREATION_CODE, AccountUtils.ACCOUNT_CREATION_MESSAGE,
                newAccount.getAccountNumber(), newAccount.getBalance(),
                customer.getFirstName() + " " + customer.getLastName());
    }

    @Transactional
    @CacheEvict(value = {"accounts", "accountByNo"}, allEntries = true)
    public BankDto deleteAccount(String accountNumber) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) {
            throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        }
        accountRepository.deleteAccountByAccountNumber(accountNumber);

        try {
            CustomerDTO customer = customerServiceClient.getCustomerById(account.getCustomerId());
            publishNotification(customer.getEmail(), "Account Closed",
                    buildAccountMessage(account, AccountUtils.ACCOUNT_DELETION_CODE, AccountUtils.ACCOUNT_DELETION_MESSAGE));
        } catch (Exception e) {
            log.warn("Could not send deletion notification for account={}: {}", accountNumber, e.getMessage());
        }

        log.info("Account deleted: accountNumber={}", accountNumber);
        return buildResponse(AccountUtils.ACCOUNT_DELETION_CODE, AccountUtils.ACCOUNT_DELETION_MESSAGE, null, null, null);
    }

    @Transactional
    @CacheEvict(value = {"accountByNo"}, key = "#accountNumber")
    public String saveAccount(String accountNumber, BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Balance cannot be negative");
        }
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if (account == null) {
            throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        }
        account.setBalance(balance);
        accountRepository.save(account);
        log.info("Balance updated: accountNumber={}, newBalance={}", accountNumber, balance);
        return "Balance updated successfully";
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────

    public BankDto createAccountFallback(AccountDTO accountDTO, Exception ex) {
        log.error("CustomerService circuit breaker triggered during account creation: {}", ex.getMessage());
        throw new BusinessRuleException("Customer service is currently unavailable. Please try again later.");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void publishNotification(String receiver, String subject, String body) {
        notificationPublisher.publish(EXCHANGE_NAME, JSON_ROUTING_KEY, receiver, subject, body);
    }

    private String buildAccountMessage(Account account, String code, String message) {
        return String.format(
                "Response Code: %s%nResponse Message: %s%nAccount Number: %s%nBalance: %s",
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
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
