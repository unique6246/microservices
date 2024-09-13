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


    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName(customer.getFirstName());
        customerDTO.setLastName(customer.getLastName());
        customerDTO.setGender(customer.getGender());
        customerDTO.setAddress(customer.getAddress());
        customerDTO.setPhoneNumber(customer.getPhoneNumber());
        customerDTO.setEmail(customer.getEmail());
        return customerDTO;
    }

    @Override
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        return customer != null ? convertToDTO(customer) : null;
    }

    @Transactional
    public BankDto deleteCustomer(Long id) {
        if(!customerRepository.existsById(id)) {
            return BankDto.builder()
                    .responseCode(CustomerUtils.CUSTOMER_NOT_EXISTS_CODE)
                    .responseMessage(CustomerUtils.CUSTOMER_NOT_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }
        Customer customer = customerRepository.findCustomerById(id);
        customerRepository.deleteCustomerById(id);
        AccountDTO accountDTO=accountServiceClient.getAccountByCustomerId(customer.getId());
        accountServiceClient.deleteAccount(accountDTO.getAccountNumber());
        notificationServiceClient.sendNotification(NotificationDTO.builder()
                .receiver(customer.getEmail())
                .subject("Customer Account Deletion")
                .body(  "Response Code = "+ CustomerUtils.CUSTOMER_DELETION_CODE+"\n\n"+
                        "Response Message = "+ CustomerUtils.CUSTOMER_DELETION_MESSAGE+"\n\n"+
                        "Account Details = Account Number:"+accountDTO.getAccountNumber()+", " +"Account Balance: "+accountDTO.getBalance()
                )
                .build());

        return BankDto.builder()
                .responseCode(CustomerUtils.CUSTOMER_DELETION_CODE)
                .responseMessage(CustomerUtils.CUSTOMER_DELETION_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .accountNumber(accountDTO.getAccountNumber())
                        .accountBalance(accountDTO.getBalance())
                        .accountName(customer.getFirstName()+" "+customer.getLastName())
                        .build())
                .build();
    }


    @Override
    public BankDto createAccount(CustomerDTO customerDTO) {

        if(customerRepository.existsByEmail(customerDTO.getEmail())) {

            return BankDto.builder()
                    .responseCode(CustomerUtils.CUSTOMER_EXISTS_CODE)
                    .responseMessage(CustomerUtils.CUSTOMER_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }
        Customer newCustomer=Customer.builder()
                .firstName(customerDTO.getFirstName())
                .lastName(customerDTO.getLastName())
                .gender(customerDTO.getGender())
                .address(customerDTO.getAddress())
                .phoneNumber(customerDTO.getPhoneNumber())
                .email(customerDTO.getEmail())
                .build();

        customerRepository.save(newCustomer);

        notificationServiceClient.sendNotification(NotificationDTO.builder()
                        .receiver(newCustomer.getEmail())
                        .subject("Welcome to our Bank")
                        .body("Response Code = "+ CustomerUtils.CUSTOMER_CREATION_CODE+"\n\n"+
                                "Response Message = "+ CustomerUtils.CUSTOMER_CREATION_MESSAGE+"\n\n")
                .build());

        return BankDto.builder()
                .responseCode(CustomerUtils.CUSTOMER_CREATION_CODE)
                .responseMessage(CustomerUtils.CUSTOMER_CREATION_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .accountName(newCustomer.getFirstName()+" "+newCustomer.getLastName())
                        .build())
                .build();
    }


}
