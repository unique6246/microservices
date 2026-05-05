package com.example.transactionservice.service;

import com.example.transactionservice.Repo.TransactionRepository;
import com.example.transactionservice.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.exception.BusinessRuleException;
import com.example.transactionservice.exception.ResourceNotFoundException;
import com.example.transactionservice.feignconfig.AccountServiceClient;
import com.example.transactionservice.feignconfig.CustomerServiceClient;
import com.example.transactionservice.service.impli.TransactionService;
import com.example.transactionservice.transactionUtils.TransactionUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final RabbitTemplate rabbitTemplate;
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final CustomerServiceClient customerServiceClient;

    @Value("${account.exchange.name}")
    private String EXCHANGE_NAME;

    @Value("${account.routing.json.key}")
    private String JSON_ROUTING_KEY;

    // ── Queries ─────────────────────────────────────────────────────────────

    public Page<TransactionDTO> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(this::convertToDTO);
    }

    public List<TransactionDTO> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByTransactionType(String transactionType) {
        return transactionRepository.findTransactionsByTransactionType(transactionType.toUpperCase()).stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "transactionFallback")
    @Retry(name = "accountService")
    public BankDto creditTransaction(TransactionDTO transactionDTO) {
        // Idempotency check
        String idempotencyKey = resolveIdempotencyKey(transactionDTO.getIdempotencyKey());
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Idempotent credit request detected for key={}", idempotencyKey);
            return buildIdempotentResponse(TransactionUtils.CREDIT_TRANSACTION_COMPLETED_CODE,
                    TransactionUtils.CREDIT_TRANSACTION_COMPLETED_MESSAGE);
        }

        AccountDTO account = getAccountOrThrow(transactionDTO.getAccountNumber());
        account.setBalance(account.getBalance().add(transactionDTO.getAmount()));

        processTransaction(account, "CREDIT", transactionDTO.getAmount(),
                "credited by the user", idempotencyKey);

        log.info("Credit completed: account={}, amount={}", transactionDTO.getAccountNumber(), transactionDTO.getAmount());
        return buildResponse(account, transactionDTO.getAmount(),
                TransactionUtils.CREDIT_TRANSACTION_COMPLETED_CODE,
                TransactionUtils.CREDIT_TRANSACTION_COMPLETED_MESSAGE, "Credit done successfully");
    }

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "transactionFallback")
    @Retry(name = "accountService")
    public BankDto debitTransaction(TransactionDTO transactionDTO) {
        String idempotencyKey = resolveIdempotencyKey(transactionDTO.getIdempotencyKey());
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Idempotent debit request detected for key={}", idempotencyKey);
            return buildIdempotentResponse(TransactionUtils.DEBIT_TRANSACTION_COMPLETED_CODE,
                    TransactionUtils.DEBIT_TRANSACTION_COMPLETED_MESSAGE);
        }

        AccountDTO account = getAccountOrThrow(transactionDTO.getAccountNumber());
        if (account.getBalance().compareTo(transactionDTO.getAmount()) < 0) {
            throw new BusinessRuleException(
                    String.format("Insufficient funds. Available: %s, Requested: %s",
                            account.getBalance(), transactionDTO.getAmount()));
        }
        account.setBalance(account.getBalance().subtract(transactionDTO.getAmount()));
        processTransaction(account, "DEBIT", transactionDTO.getAmount(), "debited by user", idempotencyKey);

        log.info("Debit completed: account={}, amount={}", transactionDTO.getAccountNumber(), transactionDTO.getAmount());
        return buildResponse(account, transactionDTO.getAmount(),
                TransactionUtils.DEBIT_TRANSACTION_COMPLETED_CODE,
                TransactionUtils.DEBIT_TRANSACTION_COMPLETED_MESSAGE, "Withdrawal done successfully");
    }

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "transferFallback")
    @Retry(name = "accountService")
    public BankDto transferBetweenUsers(TransferDTO transferDTO) {
        String idempotencyKey = resolveIdempotencyKey(transferDTO.getIdempotencyKey());
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Idempotent transfer request detected for key={}", idempotencyKey);
            return buildIdempotentResponse(TransactionUtils.TRANSACTION_COMPLETED_CODE,
                    TransactionUtils.TRANSACTION_COMPLETED_MESSAGE);
        }

        if (transferDTO.getFromAccount().equals(transferDTO.getToAccount())) {
            throw new BusinessRuleException(TransactionUtils.SELF_TRANSACTION_MESSAGE);
        }

        AccountDTO sender = getAccountOrThrow(transferDTO.getFromAccount());
        AccountDTO recipient = getAccountOrThrow(transferDTO.getToAccount());

        if (sender.getBalance().compareTo(transferDTO.getAmount()) < 0) {
            throw new BusinessRuleException(
                    String.format("Insufficient funds. Available: %s, Requested: %s",
                            sender.getBalance(), transferDTO.getAmount()));
        }

        sender.setBalance(sender.getBalance().subtract(transferDTO.getAmount()));
        recipient.setBalance(recipient.getBalance().add(transferDTO.getAmount()));

        processTransaction(sender, "DEBIT", transferDTO.getAmount(),
                "sent to " + recipient.getAccountNumber(), idempotencyKey);

        // Compensating transaction: if credit fails, restore sender balance to avoid money loss
        try {
            processTransaction(recipient, "CREDIT", transferDTO.getAmount(),
                    "received from " + sender.getAccountNumber(), UUID.randomUUID().toString());
        } catch (Exception creditEx) {
            log.error("Transfer credit step failed for account={}. Initiating compensation to restore sender={}. Cause: {}",
                    recipient.getAccountNumber(), sender.getAccountNumber(), creditEx.getMessage());
            // Restore sender balance
            AccountDTO senderCompensation = getAccountOrThrow(transferDTO.getFromAccount());
            senderCompensation.setBalance(senderCompensation.getBalance().add(transferDTO.getAmount()));
            try {
                accountServiceClient.saveAccount(senderCompensation);
                log.info("Compensation successful: restored {} to sender account={}", transferDTO.getAmount(), transferDTO.getFromAccount());
            } catch (Exception compensationEx) {
                log.error("CRITICAL: Compensation also failed for sender account={}. Manual intervention required! Cause: {}",
                        transferDTO.getFromAccount(), compensationEx.getMessage());
            }
            throw new BusinessRuleException("Transfer failed during credit step. Sender balance has been restored.");
        }

        log.info("Transfer completed: from={}, to={}, amount={}",
                transferDTO.getFromAccount(), transferDTO.getToAccount(), transferDTO.getAmount());
        return buildResponse(sender, transferDTO.getAmount(),
                TransactionUtils.TRANSACTION_COMPLETED_CODE,
                TransactionUtils.TRANSACTION_COMPLETED_MESSAGE, "Transfer successful");
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────

    public BankDto transactionFallback(TransactionDTO dto, Exception ex) {
        log.error("AccountService CB open during transaction: {}", ex.getMessage());
        throw new BusinessRuleException("Account service is temporarily unavailable. Please retry shortly.");
    }

    public BankDto transferFallback(TransferDTO dto, Exception ex) {
        log.error("AccountService CB open during transfer: {}", ex.getMessage());
        throw new BusinessRuleException("Account service is temporarily unavailable. Please retry shortly.");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void processTransaction(AccountDTO account, String type, BigDecimal amount,
                                    String message, String idempotencyKey) {
        transactionRepository.save(Transaction.builder()
                .accountNumber(account.getAccountNumber())
                .amount(amount)
                .message("Amount of " + amount + " has been " + message)
                .transactionType(type)
                .idempotencyKey(idempotencyKey)
                .build());

        accountServiceClient.saveAccount(account);

        try {
            CustomerDTO customer = customerServiceClient.getCustomerById(account.getCustomerId());
            publishNotification(customer.getEmail(),
                    type.equals("CREDIT") ? "Credit Transaction Alert" : "Debit Transaction Alert",
                    buildNotificationBody(account, amount, message));
        } catch (Exception e) {
            log.warn("Could not send transaction notification for account={}: {}", account.getAccountNumber(), e.getMessage());
        }
    }

    private AccountDTO getAccountOrThrow(String accountNumber) {
        AccountDTO account = accountServiceClient.getAccountByAccountNumber(accountNumber);
        if (account == null || account.getAccountNumber() == null) {
            throw new ResourceNotFoundException("Account", "accountNumber", accountNumber);
        }
        return account;
    }

    private String resolveIdempotencyKey(String provided) {
        return (provided != null && !provided.isBlank()) ? provided : UUID.randomUUID().toString();
    }

    private void publishNotification(String receiver, String subject, String body) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, JSON_ROUTING_KEY,
                    NotificationDTO.builder().receiver(receiver).subject(subject).body(body).build());
        } catch (Exception e) {
            log.error("Failed to publish notification: {}", e.getMessage());
        }
    }

    private String buildNotificationBody(AccountDTO account, BigDecimal amount, String message) {
        return String.format("Amount of %s has been %s%nAccount: %s%nCurrent Balance: %s",
                amount, message, account.getAccountNumber(), account.getBalance());
    }

    private BankDto buildResponse(AccountDTO account, BigDecimal amount, String code, String message, String msg) {
        return BankDto.builder()
                .responseCode(code)
                .responseMessage(message)
                .accountInfo(AccountInfo.builder()
                        .accountNumber(account.getAccountNumber())
                        .amount(amount)
                        .message(msg)
                        .build())
                .build();
    }

    private BankDto buildIdempotentResponse(String code, String message) {
        return BankDto.builder().responseCode(code).responseMessage(message + " (idempotent)").build();
    }

    private TransactionDTO convertToDTO(Transaction t) {
        return TransactionDTO.builder()
                .id(t.getId())
                .accountNumber(t.getAccountNumber())
                .amount(t.getAmount())
                .transactionType(t.getTransactionType())
                .idempotencyKey(t.getIdempotencyKey())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
