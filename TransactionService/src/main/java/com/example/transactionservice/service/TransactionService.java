package com.example.transactionservice.service;

import com.example.transactionservice.Repo.TransactionRepository;
import com.example.transactionservice.dto.*;
import com.example.transactionservice.entity.Transaction;
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

    public List<TransactionDTO> getTransactionsByTransactionType(String transactionType) {
        return transactionRepository.findTransactionsByTransactionType(transactionType).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void transfer(TransferDTO transferDTO) {
        // Debit sender's account
        Transaction debitTransaction = new Transaction();

        debitTransaction.setAccountNumber(transferDTO.getFromAccount());
        debitTransaction.setAmount(transferDTO.getAmount());
        debitTransaction.setTransactionType("DEBIT");

        //finding customer by account number
        AccountDTO debitAccount =accountServiceClient.getAccountByAccountNumber(transferDTO.getFromAccount());
        debitAccount.setBalance(debitAccount.getBalance()-debitTransaction.getAmount());
        accountServiceClient.updateBalance(debitAccount);
        CustomerDTO debitCustomer=customerServiceClient.getCustomerById(debitAccount.getCustomerId());

        //sending notification to sender
        NotificationDTO notificationSenderDTO = new NotificationDTO();
        notificationSenderDTO.setReceiver(debitCustomer.getEmail());
        notificationSenderDTO.setSubject("Debit Transaction Alert");
        notificationSenderDTO.setBody("A transaction of " + debitTransaction.getAmount() +
            " has been " + debitTransaction.getTransactionType().toLowerCase() +
            "ed from your account:"+debitAccount.getAccountNumber()+
            ".\n Account Balance: "+debitAccount.getBalance());
        notificationServiceClient.sendNotification(notificationSenderDTO);
        transactionRepository.save(debitTransaction);

        // Credit receiver's account
        Transaction creditTransaction = new Transaction();
        creditTransaction.setAccountNumber(transferDTO.getToAccount());
        creditTransaction.setAmount(transferDTO.getAmount());
        creditTransaction.setTransactionType("CREDIT");

        //finding customer by account number
        AccountDTO creditAccount =accountServiceClient.getAccountByAccountNumber(transferDTO.getToAccount());
        creditAccount.setBalance(creditAccount.getBalance()+creditTransaction.getAmount());
        accountServiceClient.updateBalance(creditAccount);
        CustomerDTO creditCustomer=customerServiceClient.getCustomerById(creditAccount.getCustomerId());

        //sending notification to receiver
        NotificationDTO notificationReceiverDTO = new NotificationDTO();
        notificationReceiverDTO.setReceiver(creditCustomer.getEmail());
        notificationReceiverDTO.setSubject("Credit Transaction Alert");
        notificationReceiverDTO.setBody("A transaction of "+creditTransaction.getAmount()+
            " has been "+creditTransaction.getTransactionType().toLowerCase()+"ed"+
            " to your account:"+creditAccount.getAccountNumber()+
            ".\nAccount Balance:"+creditAccount.getBalance());
        notificationServiceClient.sendNotification(notificationReceiverDTO);
        transactionRepository.save(creditTransaction);
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
