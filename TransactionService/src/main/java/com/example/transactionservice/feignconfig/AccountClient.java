package com.example.transactionservice.feignconfig;

import com.example.transactionservice.dto.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service")
public interface AccountClient {
    @GetMapping("/accounts/{id}")
    AccountDTO getAccountById(@PathVariable("id") Long id);
}
