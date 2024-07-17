package com.example.transactionservice.service;

import com.example.transactionservice.Repo.TransactionRepository;
import com.example.transactionservice.dto.*;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.feignconfig.AccountClient;
import com.example.transactionservice.feignconfig.AccountServiceClient;
import com.example.transactionservice.feignconfig.CustomerServiceClient;
import com.example.transactionservice.feignconfig.NotificationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private CustomerServiceClient customerServiceClient;

    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(transactionDTO.getAccountNumber());
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setTransactionType(transactionDTO.getTransactionType());
        transaction = transactionRepository.save(transaction);

        // Update account balance
        UpdateBalanceDTO updateBalanceDTO = new UpdateBalanceDTO();
        updateBalanceDTO.setAccountNumber(transactionDTO.getAccountNumber());
        updateBalanceDTO.setAmount(transactionDTO.getTransactionType().equals("DEBIT") ? -transactionDTO.getAmount() : transactionDTO.getAmount());
        accountServiceClient.updateBalance(updateBalanceDTO);

        // Send notification
        CustomerDTO customerDTO = customerServiceClient.getCustomerByAccountNumber(transactionDTO.getAccountNumber());
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setTo(customerDTO.getEmail());
        notificationDTO.setSubject("Transaction Alert");
        notificationDTO.setBody("A transaction of " + transactionDTO.getAmount() + " has been " + transactionDTO.getTransactionType() + "ed from your account.");
        notificationServiceClient.sendNotification(notificationDTO);

        return convertToDTO(transaction);
    }

    public void transfer(TransferDTO transferDTO) {
        // Debit sender's account
        TransactionDTO debitTransaction = new TransactionDTO();
        debitTransaction.setAccountNumber(transferDTO.getFromAccount());
        debitTransaction.setAmount(transferDTO.getAmount());
        debitTransaction.setTransactionType("DEBIT");
        createTransaction(debitTransaction);

        // Credit receiver's account
        TransactionDTO creditTransaction = new TransactionDTO();
        creditTransaction.setAccountNumber(transferDTO.getToAccount());
        creditTransaction.setAmount(transferDTO.getAmount());
        creditTransaction.setTransactionType("CREDIT");
        createTransaction(creditTransaction);
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setId(transaction.getId());
        transactionDTO.setAccountNumber(transaction.getAccountNumber());
        transactionDTO.setAmount(transaction.getAmount());
        transactionDTO.setTransactionType(transaction.getTransactionType());
        return transactionDTO;
    }
}
