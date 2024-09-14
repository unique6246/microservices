package com.example.coustomerservices.service;

import com.example.coustomerservices.CustomerUtils.CustomerUtils;
import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.dto.*;
import com.example.coustomerservices.entity.Customer;
import com.example.coustomerservices.feignconfig.AccountServiceClient;
import com.example.coustomerservices.feignconfig.NotificationServiceClient;
import com.example.coustomerservices.service.impli.CustomerImpl;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerImpl {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private AccountServiceClient accountServiceClient;



    @Override
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Long id) {
        return customerRepository.findById(id).map(this::convertToDTO).orElse(null);
    }

    @Transactional
    public BankDto deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            return buildResponse(CustomerUtils.CUSTOMER_NOT_EXISTS_CODE, CustomerUtils.CUSTOMER_NOT_EXISTS_MESSAGE, null, null);
        }

        Customer customer = customerRepository.findCustomerById(id);
        AccountDTO accountDTO = accountServiceClient.getAccountByCustomerId(customer.getId());

        accountServiceClient.deleteAccount(accountDTO.getAccountNumber());
        sendNotification(customer.getEmail(), "Customer Account Deletion",
                "Response Code = " + CustomerUtils.CUSTOMER_DELETION_CODE + "\n\n" +
                        "Response Message = " + CustomerUtils.CUSTOMER_DELETION_MESSAGE + "\n\n" +
                        "Account Details = Account Number:" + accountDTO.getAccountNumber() +
                        ", Account Balance: " + accountDTO.getBalance());

        customerRepository.deleteCustomerById(id);

        return buildResponse(CustomerUtils.CUSTOMER_DELETION_CODE, CustomerUtils.CUSTOMER_DELETION_MESSAGE, accountDTO, customer);
    }

    @Override
    public BankDto createAccount(CustomerDTO customerDTO) {
        if (customerRepository.existsByEmail(customerDTO.getEmail())) {
            return buildResponse(CustomerUtils.CUSTOMER_EXISTS_CODE, CustomerUtils.CUSTOMER_EXISTS_MESSAGE, null, null);
        }

        Customer newCustomer = customerRepository.save(Customer.builder()
                .firstName(customerDTO.getFirstName())
                .lastName(customerDTO.getLastName())
                .gender(customerDTO.getGender())
                .address(customerDTO.getAddress())
                .phoneNumber(customerDTO.getPhoneNumber())
                .email(customerDTO.getEmail())
                .build());

        sendNotification(newCustomer.getEmail(), "Welcome to our Bank",
                "Response Code = " + CustomerUtils.CUSTOMER_CREATION_CODE + "\n\n" +
                        "Response Message = " + CustomerUtils.CUSTOMER_CREATION_MESSAGE);

        return buildResponse(CustomerUtils.CUSTOMER_CREATION_CODE, CustomerUtils.CUSTOMER_CREATION_MESSAGE, null, newCustomer);
    }

    // Helper method for sending notifications
    private void sendNotification(String receiver, String subject, String body) {
        notificationServiceClient.sendNotification(NotificationDTO.builder()
                .receiver(receiver)
                .subject(subject)
                .body(body)
                .build());
    }

    // Helper method for building BankDto response
    private BankDto buildResponse(String code, String message, AccountDTO accountDTO, Customer customer) {
        return BankDto.builder()
                .responseCode(code)
                .responseMessage(message)
                .accountInfo(accountDTO != null ? AccountInfo.builder()
                        .accountNumber(accountDTO.getAccountNumber())
                        .accountBalance(accountDTO.getBalance())
                        .accountName(customer.getFirstName() + " " + customer.getLastName())
                        .build() : null)
                .build();
    }

    // Convert Customer to DTO
    private CustomerDTO convertToDTO(Customer customer) {
        return CustomerDTO.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .gender(customer.getGender())
                .address(customer.getAddress())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .build();
    }
}
