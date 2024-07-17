package com.example.accountservices.service;

import com.example.accountservices.Repo.AccountRepository;
import com.example.accountservices.dto.AccountDTO;
import com.example.accountservices.dto.CustomerDTO;
import com.example.accountservices.dto.NotificationDTO;
import com.example.accountservices.dto.UpdateBalanceDTO;
import com.example.accountservices.entity.Account;
import com.example.accountservices.feignconfig.CustomerClient;
import com.example.accountservices.feignconfig.CustomerServiceClient;
import com.example.accountservices.feignconfig.NotificationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerServiceClient customerServiceClient;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findById(id).orElse(null);
        return account != null ? convertToDTO(account) : null;
    }

    public AccountDTO createAccount(AccountDTO accountDTO) {
        Account account = new Account();
        account.setAccountNumber(accountDTO.getAccountNumber());
        account.setAccountType(accountDTO.getAccountType());
        account.setBalance(accountDTO.getBalance());
        account = accountRepository.save(account);

        // Send notification to customer
        CustomerDTO customerDTO = customerServiceClient.getCustomerById(accountDTO.getCustomerId());
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setTo(customerDTO.getEmail());
        notificationDTO.setSubject("New Account Created");
        notificationDTO.setBody("Dear " + customerDTO.getName() + ",\n\nYour new account with account number " + accountDTO.getAccountNumber() + " has been created successfully.");
        notificationServiceClient.sendNotification(notificationDTO);

        return convertToDTO(account);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    private AccountDTO convertToDTO(Account account) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        accountDTO.setAccountType(account.getAccountType());
        accountDTO.setAccountNumber(account.getAccountNumber());
        accountDTO.setBalance(account.getBalance());
        accountDTO.setCustomerId(account.getCustomerId());
        return accountDTO;
    }
}
