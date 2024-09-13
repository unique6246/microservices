package com.example.accountservices.service;

import com.example.accountservices.Repo.AccountRepository;
import com.example.accountservices.dto.*;
import com.example.accountservices.entity.Account;
import com.example.accountservices.entity.AccountUtils;
import com.example.accountservices.feignconfig.CustomerServiceClient;
import com.example.accountservices.feignconfig.NotificationServiceClient;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    //all accounts
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    //account by id
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findById(id).orElse(null);
        return account != null ? convertToDTO(account) : null;
    }

    //account by customer id
    public AccountDTO getAccountByCustomerId(Long id) {
        if(accountRepository.existsAccountByCustomerId(id)) {
            Account account=  accountRepository.findAccountByCustomerId(id);
            return convertToDTO(account);
        }
        return AccountDTO.builder()
                .accountNumber(null)
                .balance(BigDecimal.ZERO)
                .accountType(null)
                .build();
    }

    //account by account number
    public AccountDTO getAccountByAccountNumber(String accountNumber) {
        Account account=  accountRepository.findAccountByAccountNumber(accountNumber);
        return convertToDTO(account);
    }

    //account creation
    public BankDto createAccount(AccountDTO accountDTO) {

        if(accountRepository.existsAccountByAccountNumber(accountDTO.getAccountNumber())) {
            return BankDto.builder()
                    .responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_EXISTS_MESSAGE)
                    .accountInfo(AccountInfo.builder()
                            .accountNumber(accountDTO.getAccountNumber())
                            .build())
                    .build();
        }
        Account newCustomer=Account.builder()
                .accountNumber(AccountUtils.generateAccountNumber())
                .accountType(accountDTO.getAccountType())
                .balance(BigDecimal.ZERO)
                .customerId(accountDTO.getCustomerId())
                .build();

        accountRepository.save(newCustomer);

        CustomerDTO customerDTO=customerServiceClient.getCustomerById(newCustomer.getCustomerId());


        notificationServiceClient.sendNotification(NotificationDTO.builder()
                .receiver(customerDTO.getEmail())
                .subject("Welcome to our Bank")
                .body("Response Code = "+AccountUtils.ACCOUNT_CREATION_CODE+"\n\n"+
                        "Response Message = "+AccountUtils.ACCOUNT_CREATION_MESSAGE+"\n\n"+
                        "Account Details = Account Number:"+newCustomer.getAccountNumber()+", "
                        +"Account Balance: "+newCustomer.getBalance()
                )
                .build());

        return BankDto.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .accountBalance(newCustomer.getBalance())
                        .accountNumber(newCustomer.getAccountNumber())
                        .accountName(customerDTO.getFirstName()+" "+customerDTO.getLastName())
                        .build())
                .build();
    }

    //account deletion
    @Transactional
    public BankDto deleteAccount(String accountNumber) {
        if(!accountRepository.existsAccountByAccountNumber(accountNumber)) {
            return BankDto.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        accountRepository.deleteAccountByAccountNumber(accountNumber);
        CustomerDTO customerDTO=customerServiceClient.getCustomerById(account.getCustomerId());

        notificationServiceClient.sendNotification(NotificationDTO.builder()
                .receiver(customerDTO.getEmail())
                .subject("Account Deletion")
                .body(  "Response Code = "+AccountUtils.ACCOUNT_DELETION_CODE+"\n\n"+
                        "Response Message = "+AccountUtils.ACCOUNT_DELETION_MESSAGE+"\n\n"+
                        "Account Details = Account Number:"+account.getAccountNumber()+", " +"Account Balance: "+account.getBalance()
                )
                .build());

        return BankDto.builder()
                .responseCode(AccountUtils.ACCOUNT_DELETION_CODE)
                .responseMessage(AccountUtils.ACCOUNT_DELETION_MESSAGE)
                .accountInfo(null)
                .build();
    }

    private AccountDTO convertToDTO(Account account) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setAccountType(account.getAccountType());
        accountDTO.setAccountNumber(account.getAccountNumber());
        accountDTO.setBalance(account.getBalance());
        accountDTO.setCustomerId(account.getCustomerId());
        return accountDTO;
    }
}
