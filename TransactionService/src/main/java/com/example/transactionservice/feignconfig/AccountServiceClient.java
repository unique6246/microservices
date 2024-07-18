package com.example.transactionservice.feignconfig;

import com.example.transactionservice.dto.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("ACCOUNT-SERVICE")
public interface AccountServiceClient {

    @GetMapping("/accounts/{id}")
    AccountDTO getAccountById(@PathVariable("id") Long id);

    @PutMapping("/accounts/update-balance")
    void updateBalance(@RequestBody AccountDTO accountDTO);

    @GetMapping("/accounts/account/{accountNumber}")
    AccountDTO getAccountByAccountNumber(@PathVariable String accountNumber);
}
