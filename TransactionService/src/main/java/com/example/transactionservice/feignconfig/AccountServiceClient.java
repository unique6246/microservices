package com.example.transactionservice.feignconfig;


import com.example.transactionservice.dto.AccountDTO;
import com.example.transactionservice.dto.BankDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient("ACCOUNT-SERVICE")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/customer/{id}")
    AccountDTO getAccountByCustomerId(@PathVariable Long id);

    @GetMapping("/api/v1/accounts/account/{accountNumber}")
    AccountDTO getAccountByAccountNumber(@PathVariable String accountNumber);

    @DeleteMapping("/api/v1/accounts/{accountNumber}")
    BankDto deleteAccount(@PathVariable String accountNumber);

    @PutMapping("/api/v1/accounts/update")
    void saveAccount(@RequestBody AccountDTO accountDTO);
}