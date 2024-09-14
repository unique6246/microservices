package com.example.transactionservice.service;

import com.example.transactionservice.Repo.TransactionRepository;
import com.example.transactionservice.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.feignconfig.AccountServiceClient;
import com.example.transactionservice.feignconfig.CustomerServiceClient;
import com.example.transactionservice.feignconfig.NotificationServiceClient;
import com.example.transactionservice.service.impli.TransactionService;
import com.example.transactionservice.transactionUtils.TransactionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private CustomerServiceClient customerServiceClient;

    // Get all transactions
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Get transactions by account number
    public List<TransactionDTO> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Get transactions by type
    public List<TransactionDTO> getTransactionsByTransactionType(String transactionType) {
        return transactionRepository.findTransactionsByTransactionType(transactionType).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Credit transaction
    public BankDto creditTransaction(TransactionDTO transactionDTO) {
        AccountDTO account = accountServiceClient.getAccountByAccountNumber(transactionDTO.getAccountNumber());
        account.setBalance(account.getBalance().add(transactionDTO.getAmount()));


        processTransaction(account, "CREDIT-ADD", transactionDTO.getAmount(), "credited");

        return buildResponse(account, transactionDTO.getAmount(), TransactionUtils.CREDIT_TRANSACTION_COMPLETED_CODE,
                TransactionUtils.CREDIT_TRANSACTION_COMPLETED_MESSAGE, "Credit done successfully");
    }

    // Debit transaction
    public BankDto debitTransaction(TransactionDTO transactionDTO) {
        AccountDTO account = accountServiceClient.getAccountByAccountNumber(transactionDTO.getAccountNumber());

        if (account.getBalance().compareTo(transactionDTO.getAmount()) >= 0) {
            account.setBalance(account.getBalance().subtract(transactionDTO.getAmount()));


            processTransaction(account, "DEBIT-WITHDRAW", transactionDTO.getAmount(), "debited");
            return buildResponse(account, transactionDTO.getAmount(), TransactionUtils.DEBIT_TRANSACTION_COMPLETED_CODE,
                    TransactionUtils.DEBIT_TRANSACTION_COMPLETED_MESSAGE, "Withdrawal done successfully");
        }

        return buildResponse(account, transactionDTO.getAmount(), TransactionUtils.DEBIT_TRANSACTION_FAILED_CODE,
                TransactionUtils.DEBIT_TRANSACTION_FAILED_MESSAGE, "Withdrawal failed");
    }

    // Transfer between users
    public BankDto transferBetweenUsers(TransferDTO transferDTO) {
        AccountDTO senderAccount = accountServiceClient.getAccountByAccountNumber(transferDTO.getFromAccount());
        AccountDTO recipientAccount = accountServiceClient.getAccountByAccountNumber(transferDTO.getToAccount());

        if (senderAccount.getBalance().compareTo(transferDTO.getAmount()) >= 0) {
            senderAccount.setBalance(senderAccount.getBalance().subtract(transferDTO.getAmount()));
            recipientAccount.setBalance(recipientAccount.getBalance().add(transferDTO.getAmount()));

            processTransaction(senderAccount, "DEBIT", transferDTO.getAmount(), "sent to " + recipientAccount.getAccountNumber());
            processTransaction(recipientAccount, "CREDIT", transferDTO.getAmount(), "received from " + senderAccount.getAccountNumber());

            notifyTransfer(senderAccount, recipientAccount, transferDTO.getAmount());

            return buildResponse(senderAccount, transferDTO.getAmount(), TransactionUtils.TRANSACTION_COMPLETED_CODE,
                    TransactionUtils.TRANSACTION_COMPLETED_MESSAGE, "Amount transferred to receiver");
        }

        return buildResponse(senderAccount, senderAccount.getBalance(), TransactionUtils.LOW_BALANCE_CODE + " AND " + TransactionUtils.TRANSACTION_FAILED_CODE,
                TransactionUtils.LOW_BALANCE_MESSAGE + ", " + TransactionUtils.TRANSACTION_FAILED_MESSAGE, "Amount not transferred to receiver");
    }

    // Helper Methods
    private void processTransaction(AccountDTO account, String type, BigDecimal amount, String message) {
        transactionRepository.save(Transaction.builder()
                .accountNumber(account.getAccountNumber())
                .amount(account.getBalance())
                .message("Amount of " + amount + " has been " + message)
                .transactionType(type)
                .build());
        accountServiceClient.saveAccount(account);

        CustomerDTO customer = customerServiceClient.getCustomerById(account.getCustomerId());
        sendNotification(customer.getEmail(), type.toUpperCase() + " TRANSACTION ALERT",
                "Amount of " + amount + " has been " + message + " from your account.");
    }

    private void notifyTransfer(AccountDTO sender, AccountDTO recipient, BigDecimal amount) {
        CustomerDTO senderCustomer = customerServiceClient.getCustomerById(sender.getCustomerId());
        CustomerDTO recipientCustomer = customerServiceClient.getCustomerById(recipient.getCustomerId());

        sendNotification(senderCustomer.getEmail(), "DEBIT TRANSACTION ALERT",
                buildTransferMessage(amount, recipientCustomer, "debited"));
        sendNotification(recipientCustomer.getEmail(), "CREDIT TRANSACTION ALERT",
                buildTransferMessage(amount, senderCustomer, "credited"));
    }

    private String buildTransferMessage(BigDecimal amount, CustomerDTO otherCustomer, String action) {
        return "Amount of " + amount + " has been " + action + ".\n\n" +
                "Details: Name: " + otherCustomer.getFirstName() + " " + otherCustomer.getLastName() +
                ", Phone: " + otherCustomer.getPhoneNumber() + ", Email: " + otherCustomer.getEmail();
    }

    private void sendNotification(String receiver, String subject, String body) {
        notificationServiceClient.sendNotification(NotificationDTO.builder()
                .receiver(receiver)
                .subject(subject)
                .body(body)
                .build());
    }

    private BankDto buildResponse(AccountDTO account, BigDecimal amount, String responseCode, String responseMessage, String message) {
        return BankDto.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .accountInfo(AccountInfo.builder()
                        .accountNumber(account.getAccountNumber())
                        .amount(amount)
                        .message(message)
                        .build())
                .build();
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .accountNumber(transaction.getAccountNumber())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .build();
    }
}
