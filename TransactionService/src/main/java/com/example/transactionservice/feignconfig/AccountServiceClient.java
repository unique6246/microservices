package com.example.transactionservice.feignconfig;

import com.example.transactionservice.dto.UpdateBalanceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("account-service")
public interface AccountServiceClient {

    @PostMapping("/accounts/updateBalance")
    void updateBalance(@RequestBody UpdateBalanceDTO updateBalanceDTO);
}
