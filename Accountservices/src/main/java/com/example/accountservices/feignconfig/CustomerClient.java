package com.example.accountservices.feignconfig;

import com.example.accountservices.dto.CustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {
    @GetMapping("/customers/{id}")
    CustomerDTO getCustomerById(@PathVariable("id") Long id);
}
