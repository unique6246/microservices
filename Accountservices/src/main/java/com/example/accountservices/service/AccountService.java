package com.example.accountservices.service;

import com.example.accountservices.Repo.AccountRepository;
import com.example.accountservices.dto.AccountDTO;
import com.example.accountservices.dto.CustomerDTO;
import com.example.accountservices.dto.NotificationDTO;
import com.example.accountservices.entity.Account;
import com.example.accountservices.entity.AccountUtils;
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

    public AccountDTO getAccountByCustomerId(Long id) {
        Account account=  accountRepository.findAccountByCustomerId(id);
        return convertToDTO(account);
    }
    public AccountDTO getAccountByAccountNumber(String accountNumber) {
        Account account=  accountRepository.findAccountByAccountNumber(accountNumber);
        return convertToDTO(account);
    }

    public void createAccount(AccountDTO accountDTO) {

        Account account = new Account();
        String accNum=AccountUtils.generateAccountNumber();
        account.setAccountNumber(accNum);
        account.setAccountType(accountDTO.getAccountType());
        account.setBalance(accountDTO.getBalance());
        account.setCustomerId(accountDTO.getCustomerId());

        // Send notification to customer
        CustomerDTO customerDTO = customerServiceClient.getCustomerById(accountDTO.getCustomerId());
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setReceiver(customerDTO.getEmail());
        notificationDTO.setSubject("New Account Created");
        notificationDTO.setBody("Dear " + customerDTO.getName() + ",\n\nYour new account with account number " + accNum + " has been created successfully.");
        notificationServiceClient.sendNotification(notificationDTO);
        accountRepository.save(account);
    }

    public void updateBalance(AccountDTO accountDTO) {
        Account account = accountRepository.findAccountByAccountNumber(accountDTO.getAccountNumber());
        account.setBalance(accountDTO.getBalance());
        accountRepository.save(account);
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
