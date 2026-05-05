package com.example.accountservices.service.impli;

import com.example.accountservices.dto.AccountDTO;
import com.example.accountservices.dto.BankDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface AccountServiceImpl {
    Page<AccountDTO> getAllAccounts(Pageable pageable);
    AccountDTO getAccountById(Long id);
    AccountDTO getAccountByCustomerId(Long customerId);
    AccountDTO getAccountByAccountNumber(String accountNumber);
    BankDto createAccount(AccountDTO accountDTO);
    BankDto deleteAccount(String accountNumber);
    String saveAccount(String accountNumber, BigDecimal balance);
}
