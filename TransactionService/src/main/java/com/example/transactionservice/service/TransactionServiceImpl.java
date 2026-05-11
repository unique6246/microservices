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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final RabbitTemplate            rabbitTemplate;
    private final TransactionRepository     transactionRepository;
    private final AccountServiceClient      accountServiceClient;
    private final CustomerServiceClient     customerServiceClient;
    private final FraudDetectionService     fraudDetectionService;

    @Value("${account.exchange.name}")
    private String EXCHANGE_NAME;

    @Value("${account.routing.json.key}")
    private String JSON_ROUTING_KEY;

    private static final Random RANDOM = new Random();

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

    public TransactionDTO getByReferenceNumber(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceNumber", referenceNumber));
    }

    public Page<TransactionDTO> getFlaggedTransactions(Pageable pageable) {
        return transactionRepository.findByFraudFlagTrue(pageable).map(this::convertToDTO);
    }

    public List<TransactionDTO> getStatement(String accountNumber, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtAsc(accountNumber, from, to)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "transactionFallback")
    @Retry(name = "accountService")
    public BankDto creditTransaction(TransactionDTO transactionDTO) {
        String idempotencyKey = resolveIdempotencyKey(transactionDTO.getIdempotencyKey());
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Idempotent credit request detected for key={}", idempotencyKey);
            return buildIdempotentResponse(TransactionUtils.CREDIT_TRANSACTION_COMPLETED_CODE,
                    TransactionUtils.CREDIT_TRANSACTION_COMPLETED_MESSAGE);
        }

        AccountDTO account = getAccountOrThrow(transactionDTO.getAccountNumber());
        validateAccountActive(account);
        account.setBalance(account.getBalance().add(transactionDTO.getAmount()));

        Transaction saved = processTransaction(account, "CREDIT", transactionDTO.getAmount(),
                "credited by the user", idempotencyKey, "INTERNAL", BigDecimal.ZERO,
                null, null, null, null, null);
        runFraudCheck(saved);

        log.info("Credit completed: account={}, amount={}, ref={}",
                transactionDTO.getAccountNumber(), transactionDTO.getAmount(), saved.getReferenceNumber());
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
        validateAccountActive(account);
        validateSufficientBalance(account, transactionDTO.getAmount());
        validateDailyLimit(account, transactionDTO.getAmount());
        validateMinBalance(account, transactionDTO.getAmount());

        account.setBalance(account.getBalance().subtract(transactionDTO.getAmount()));
        Transaction saved = processTransaction(account, "DEBIT", transactionDTO.getAmount(),
                "debited by user", idempotencyKey, "INTERNAL", BigDecimal.ZERO,
                null, null, null, null, null);
        runFraudCheck(saved);

        log.info("Debit completed: account={}, amount={}, ref={}",
                transactionDTO.getAccountNumber(), transactionDTO.getAmount(), saved.getReferenceNumber());
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

        AccountDTO sender    = getAccountOrThrow(transferDTO.getFromAccount());
        AccountDTO recipient = getAccountOrThrow(transferDTO.getToAccount());
        validateAccountActive(sender);
        validateSufficientBalance(sender, transferDTO.getAmount());
        validateDailyLimit(sender, transferDTO.getAmount());
        validateMinBalance(sender, transferDTO.getAmount());

        sender.setBalance(sender.getBalance().subtract(transferDTO.getAmount()));
        recipient.setBalance(recipient.getBalance().add(transferDTO.getAmount()));

        Transaction debitTxn = processTransaction(sender, "DEBIT", transferDTO.getAmount(),
                "sent to " + recipient.getAccountNumber(), idempotencyKey, "INTERNAL", BigDecimal.ZERO,
                recipient.getAccountNumber(), null, null, null, null);

        try {
            processTransaction(recipient, "CREDIT", transferDTO.getAmount(),
                    "received from " + sender.getAccountNumber(), UUID.randomUUID().toString(),
                    "INTERNAL", BigDecimal.ZERO, sender.getAccountNumber(), null, null, null, null);
        } catch (Exception creditEx) {
            log.error("Transfer credit step failed for account={}. Compensating sender={}",
                    recipient.getAccountNumber(), sender.getAccountNumber());
            AccountDTO senderComp = getAccountOrThrow(transferDTO.getFromAccount());
            senderComp.setBalance(senderComp.getBalance().add(transferDTO.getAmount()));
            try {
                accountServiceClient.saveAccount(senderComp);
            } catch (Exception e) {
                log.error("CRITICAL: Compensation also failed for sender account={}", transferDTO.getFromAccount());
            }
            throw new BusinessRuleException("Transfer failed during credit step. Sender balance has been restored.");
        }

        runFraudCheck(debitTxn);

        log.info("Transfer completed: from={}, to={}, amount={}",
                transferDTO.getFromAccount(), transferDTO.getToAccount(), transferDTO.getAmount());
        return buildResponse(sender, transferDTO.getAmount(),
                TransactionUtils.TRANSACTION_COMPLETED_CODE,
                TransactionUtils.TRANSACTION_COMPLETED_MESSAGE, "Transfer successful");
    }

    // ── NEFT / RTGS / IMPS ───────────────────────────────────────────────────

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "neftFallback")
    public BankDto neftRtgsTransfer(NeftRtgsDTO dto) {
        if ("RTGS".equals(dto.getChannel()) && dto.getAmount().compareTo(new BigDecimal("200000")) < 0) {
            throw new BusinessRuleException("RTGS minimum transfer amount is ₹2,00,000");
        }

        String idempotencyKey = resolveIdempotencyKey(dto.getIdempotencyKey());
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return buildIdempotentResponse("TXN_200", "Transfer already processed (idempotent)");
        }

        AccountDTO account = getAccountOrThrow(dto.getFromAccount());
        validateAccountActive(account);
        validateSufficientBalance(account, dto.getAmount());
        validateDailyLimit(account, dto.getAmount());
        validateMinBalance(account, dto.getAmount());

        BigDecimal fee = calculateFee(dto.getChannel(), dto.getAmount());
        BigDecimal totalDeduct = dto.getAmount().add(fee);
        account.setBalance(account.getBalance().subtract(totalDeduct));

        Transaction saved = processTransaction(account, "DEBIT", dto.getAmount(),
                dto.getChannel() + " transfer to " + dto.getBeneficiaryName(), idempotencyKey,
                dto.getChannel(), fee,
                dto.getBeneficiaryAccountNumber(), dto.getBeneficiaryName(),
                dto.getIfscCode(), dto.getBankName(), dto.getRemarks());
        runFraudCheck(saved);

        log.info("{} transfer: from={}, ref={}, amount={}, fee={}",
                dto.getChannel(), dto.getFromAccount(), saved.getReferenceNumber(), dto.getAmount(), fee);
        return buildResponse(account, dto.getAmount(), "TXN_200",
                dto.getChannel() + " transfer initiated successfully",
                "Reference: " + saved.getReferenceNumber() + " | Fee: ₹" + fee);
    }

    // ── UPI ──────────────────────────────────────────────────────────────────

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "upiTransferFallback")
    public BankDto upiTransfer(UpiDTO dto) {
        String idempotencyKey = resolveIdempotencyKey(dto.getIdempotencyKey());
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return buildIdempotentResponse("TXN_200", "UPI payment already processed (idempotent)");
        }

        AccountDTO account = getAccountOrThrow(dto.getFromAccount());
        validateAccountActive(account);
        validateSufficientBalance(account, dto.getAmount());
        validateDailyLimit(account, dto.getAmount());
        validateMinBalance(account, dto.getAmount());

        account.setBalance(account.getBalance().subtract(dto.getAmount()));

        Transaction saved = processTransaction(account, "DEBIT", dto.getAmount(),
                "UPI transfer to " + dto.getUpiId(), idempotencyKey,
                "UPI", BigDecimal.ZERO,
                null, dto.getUpiId(), null, null, dto.getRemarks());
        // Set upiId on the saved transaction
        saved.setUpiId(dto.getUpiId());
        transactionRepository.save(saved);

        runFraudCheck(saved);

        log.info("UPI transfer: from={}, upi={}, amount={}, ref={}",
                dto.getFromAccount(), dto.getUpiId(), dto.getAmount(), saved.getReferenceNumber());
        return buildResponse(account, dto.getAmount(), "TXN_200",
                "UPI payment sent successfully",
                "Reference: " + saved.getReferenceNumber());
    }

    // ── Statement ────────────────────────────────────────────────────────────

    public StatementSummaryDTO getStatementSummary(String accountNumber, LocalDateTime from, LocalDateTime to) {
        List<Transaction> txns = transactionRepository
                .findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtAsc(accountNumber, from, to);

        BigDecimal totalCredits = txns.stream()
                .filter(t -> "CREDIT".equals(t.getTransactionType()) && "COMPLETED".equals(t.getStatus()))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = txns.stream()
                .filter(t -> "DEBIT".equals(t.getTransactionType()) && "COMPLETED".equals(t.getStatus()))
                .map(t -> t.getAmount().add(t.getProcessingFee())).reduce(BigDecimal.ZERO, BigDecimal::add);

        return StatementSummaryDTO.builder()
                .accountNumber(accountNumber)
                .fromDate(from.toLocalDate())
                .toDate(to.toLocalDate())
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .netAmount(totalCredits.subtract(totalDebits))
                .transactionCount(txns.size())
                .build();
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

    public BankDto neftFallback(NeftRtgsDTO dto, Exception ex) {
        log.error("AccountService CB open during NEFT/RTGS: {}", ex.getMessage());
        throw new BusinessRuleException("Transfer service is temporarily unavailable. Please retry shortly.");
    }

    public BankDto upiTransferFallback(UpiDTO dto, Exception ex) {
        log.error("AccountService CB open during UPI: {}", ex.getMessage());
        throw new BusinessRuleException("UPI service is temporarily unavailable. Please retry shortly.");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private Transaction processTransaction(AccountDTO account, String type, BigDecimal amount,
                                           String message, String idempotencyKey, String channel,
                                           BigDecimal fee, String beneficiaryAccount,
                                           String beneficiaryName, String beneficiaryIfsc,
                                           String bankName, String remarks) {
        String refNumber = generateReferenceNumber();

        Transaction txn = transactionRepository.save(Transaction.builder()
                .accountNumber(account.getAccountNumber())
                .amount(amount)
                .message("Amount of " + amount + " has been " + message)
                .transactionType(type)
                .idempotencyKey(idempotencyKey)
                .channel(channel)
                .status("COMPLETED")
                .referenceNumber(refNumber)
                .beneficiaryAccount(beneficiaryAccount)
                .beneficiaryName(beneficiaryName)
                .beneficiaryIfsc(beneficiaryIfsc)
                .bankName(bankName)
                .processingFee(fee != null ? fee : BigDecimal.ZERO)
                .remarks(remarks)
                .build());

        accountServiceClient.saveAccount(account);

        try {
            CustomerDTO customer = customerServiceClient.getCustomerById(account.getCustomerId());
            publishNotification(customer.getEmail(),
                    type.equals("CREDIT") ? "Credit Alert - " + channel : "Debit Alert - " + channel,
                    buildNotificationBody(account, amount, message, refNumber, fee));
        } catch (Exception e) {
            log.warn("Could not send transaction notification for account={}: {}", account.getAccountNumber(), e.getMessage());
        }

        return txn;
    }

    private void runFraudCheck(Transaction txn) {
        try {
            FraudDetectionService.FraudCheckResult result = fraudDetectionService.check(txn);
            if (result.isFlagged()) {
                txn.setFraudFlag(true);
                txn.setFraudReason(String.join("; ", result.getTriggeredRules()));
                txn.setFraudSeverity(result.getSeverity());
                transactionRepository.save(txn);
                log.warn("Fraud flagged on ref={} severity={}", txn.getReferenceNumber(), result.getSeverity());
            }
        } catch (Exception e) {
            log.error("Fraud check failed for ref={}: {}", txn.getReferenceNumber(), e.getMessage());
        }
    }

    private void validateAccountActive(AccountDTO account) {
        if (account.getStatus() != null && !"ACTIVE".equals(account.getStatus())) {
            throw new BusinessRuleException("Account " + account.getAccountNumber() +
                    " is not active. Status: " + account.getStatus());
        }
    }

    private void validateSufficientBalance(AccountDTO account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException(String.format(
                    "Insufficient funds. Available: ₹%s, Requested: ₹%s",
                    account.getBalance(), amount));
        }
    }

    private void validateDailyLimit(AccountDTO account, BigDecimal amount) {
        if (account.getDailyTxnLimit() != null && account.getUsedDailyAmount() != null) {
            BigDecimal remaining = account.getDailyTxnLimit().subtract(account.getUsedDailyAmount());
            if (amount.compareTo(remaining) > 0) {
                throw new BusinessRuleException(String.format(
                        "Daily transaction limit exceeded. Remaining today: ₹%s, Requested: ₹%s",
                        remaining, amount));
            }
        }
    }

    private void validateMinBalance(AccountDTO account, BigDecimal amount) {
        if (account.getMinBalance() != null) {
            BigDecimal balanceAfter = account.getBalance().subtract(amount);
            if (balanceAfter.compareTo(account.getMinBalance()) < 0) {
                throw new BusinessRuleException(String.format(
                        "Transaction would breach minimum balance of ₹%s. Current: ₹%s, After: ₹%s",
                        account.getMinBalance(), account.getBalance(), balanceAfter));
            }
        }
    }

    private BigDecimal calculateFee(String channel, BigDecimal amount) {
        return switch (channel) {
            case "RTGS" -> new BigDecimal("24.00");
            case "IMPS" -> amount.multiply(new BigDecimal("0.005"))
                    .min(new BigDecimal("15.00")).setScale(2, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO; // NEFT, UPI, INTERNAL are free
        };
    }

    private String generateReferenceNumber() {
        String ts     = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = String.format("%06d", RANDOM.nextInt(1_000_000));
        return "TXN-" + ts + "-" + suffix;
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

    private String buildNotificationBody(AccountDTO account, BigDecimal amount,
                                         String message, String ref, BigDecimal fee) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Amount of ₹%s has been %s%n", amount, message));
        sb.append(String.format("Account  : %s%n", account.getAccountNumber()));
        sb.append(String.format("Balance  : ₹%s%n", account.getBalance()));
        sb.append(String.format("Reference: %s%n", ref));
        if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Fee      : ₹%s%n", fee));
        }
        return sb.toString();
    }

    private BankDto buildResponse(AccountDTO account, BigDecimal amount, String code,
                                  String message, String msg) {
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
                .channel(t.getChannel())
                .status(t.getStatus())
                .referenceNumber(t.getReferenceNumber())
                .beneficiaryAccount(t.getBeneficiaryAccount())
                .beneficiaryName(t.getBeneficiaryName())
                .processingFee(t.getProcessingFee())
                .fraudFlag(t.getFraudFlag())
                .remarks(t.getRemarks())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
