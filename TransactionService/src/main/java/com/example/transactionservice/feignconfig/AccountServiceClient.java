package com.example.transactionservice.feignconfig;

import com.example.transactionservice.dto.AccountDTO;
import com.example.transactionservice.dto.UpdateBalanceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("ACCOUNT-SERVICE")
public interface AccountServiceClient {

    @GetMapping("/accounts/{id}")
    AccountDTO getAccountById(@PathVariable("id") Long id);

    @PostMapping("/accounts/updateBalance")
    void updateBalance(@RequestBody UpdateBalanceDTO updateBalanceDTO);
}
