package com.example.coustomerservices.service.impli;

import com.example.coustomerservices.dto.BankDto;
import com.example.coustomerservices.dto.CustomerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerImpl {
    Page<CustomerDTO> getAllCustomers(Pageable pageable);
    BankDto createAccount(CustomerDTO customerDTO);
    CustomerDTO getCustomerById(Long id);
    BankDto deleteCustomer(Long id);
}
