package com.example.coustomerservices.service;

import com.example.coustomerservices.AccountUtils.AccountUtils;
import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.dto.AccountInfo;
import com.example.coustomerservices.dto.BankDto;
import com.example.coustomerservices.dto.CustomerDTO;
import com.example.coustomerservices.dto.NotificationDTO;
import com.example.coustomerservices.entity.Customer;
import com.example.coustomerservices.feignconfig.NotificationServiceClient;
import com.example.coustomerservices.service.impli.CustomerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerImpl {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

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

    public BankDto deleteCustomer(Long id) {
        if(!customerRepository.existsById(id)) {
            return BankDto.builder()
                    .responseCode(AccountUtils.ACCOUNT_NOT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_NOT_EXISTS_MESSAGE)
                    .accountInfo(null)
                    .build();
        }
        Customer customer = customerRepository.findById(id).orElse(null);
        customerRepository.deleteById(id);

        notificationServiceClient.sendNotification(NotificationDTO.builder()
                .receiver(customer.getEmail())
                .subject("Account Deletion")
                .body(  "Response Code = "+AccountUtils.ACCOUNT_DELETION_CODE+"\n\n"+
                        "Response Message = "+AccountUtils.ACCOUNT_DELETION_MESSAGE+"\n\n"+
                        "Account Details = Account Number:"+customer.getAccountNumber()+", " +"Account Balance: "+customer.getBalance()
                )
                .build());

        return BankDto.builder()
                .responseCode(AccountUtils.ACCOUNT_DELETION_CODE)
                .responseMessage(AccountUtils.ACCOUNT_DELETION_MESSAGE)
                .accountInfo(null)
                .build();
    }


    @Override
    public BankDto createAccount(CustomerDTO customerDTO) {

        if(customerRepository.existsByEmail(customerDTO.getEmail())) {
            return BankDto.builder()
                    .responseCode(AccountUtils.ACCOUNT_EXISTS_CODE)
                    .responseMessage(AccountUtils.ACCOUNT_EXISTS_MESSAGE)
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
                .accountNumber(AccountUtils.generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .build();

        Customer savedCustomer=customerRepository.save(newCustomer);


        notificationServiceClient.sendNotification(NotificationDTO.builder()
                        .receiver(savedCustomer.getEmail())
                        .subject("Welcome to our Bank")
                        .body("Response Code = "+AccountUtils.ACCOUNT_CREATION_CODE+"\n\n"+
                                "Response Message = "+AccountUtils.ACCOUNT_CREATION_MESSAGE+"\n\n"+
                                "Account Details = Account Number:"+savedCustomer.getAccountNumber()+", " +"Account Balance: "+savedCustomer.getBalance()
                        )

                .build());

        return BankDto.builder()
                .responseCode(AccountUtils.ACCOUNT_CREATION_CODE)
                .responseMessage(AccountUtils.ACCOUNT_CREATION_MESSAGE)
                .accountInfo(AccountInfo.builder()
                        .accountBalance(savedCustomer.getBalance())
                        .accountNumber(savedCustomer.getAccountNumber())
                        .accountName(savedCustomer.getFirstName()+" "+savedCustomer.getLastName())
                        .build())
                .build();
    }


}
