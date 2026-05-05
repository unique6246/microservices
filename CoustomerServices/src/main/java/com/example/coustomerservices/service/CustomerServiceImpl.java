package com.example.coustomerservices.service;

import com.example.coustomerservices.CustomerUtils.CustomerUtils;
import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.dto.*;
import com.example.coustomerservices.entity.Customer;
import com.example.coustomerservices.exception.BusinessRuleException;
import com.example.coustomerservices.exception.DuplicateResourceException;
import com.example.coustomerservices.exception.ResourceNotFoundException;
import com.example.coustomerservices.config.NotificationPublisher;
import com.example.coustomerservices.feignconfig.AccountServiceClient;
import com.example.coustomerservices.service.impli.CustomerImpl;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerImpl {

    private final NotificationPublisher notificationPublisher;
    private final CustomerRepository customerRepository;
    private final AccountServiceClient accountServiceClient;

    @Value("${account.exchange.name}")
    private String EXCHANGE_NAME;

    @Value("${account.routing.json.key}")
    private String JSON_ROUTING_KEY;

    // ── Queries ─────────────────────────────────────────────────────────────

    public Page<CustomerDTO> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Cacheable(value = "customers", key = "#id")
    public CustomerDTO getCustomerById(Long id) {
        return customerRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public BankDto createAccount(CustomerDTO customerDTO) {
        if (customerRepository.existsByEmail(customerDTO.getEmail())) {
            throw new DuplicateResourceException("Customer already exists with email: " + customerDTO.getEmail());
        }

        Customer newCustomer = customerRepository.save(Customer.builder()
                .firstName(customerDTO.getFirstName())
                .lastName(customerDTO.getLastName())
                .gender(customerDTO.getGender())
                .address(customerDTO.getAddress())
                .phoneNumber(customerDTO.getPhoneNumber())
                .email(customerDTO.getEmail())
                .build());

        publishNotification(newCustomer.getEmail(), "Welcome to Our Bank",
                buildWelcomeMessage(newCustomer));

        log.info("Customer registered: id={}, email={}", newCustomer.getId(), newCustomer.getEmail());
        return buildResponse(CustomerUtils.CUSTOMER_CREATION_CODE, CustomerUtils.CUSTOMER_CREATION_MESSAGE, null, newCustomer);
    }

    @Transactional
    @CircuitBreaker(name = "accountService", fallbackMethod = "deleteCustomerFallback")
    @Retry(name = "accountService")
    @CacheEvict(value = "customers", key = "#id")
    public BankDto deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        AccountDTO accountDTO = null;
        try {
            accountDTO = accountServiceClient.getAccountByCustomerId(customer.getId());
            if (accountDTO != null && accountDTO.getAccountNumber() != null) {
                accountServiceClient.deleteAccount(accountDTO.getAccountNumber());
            }
        } catch (Exception e) {
            log.warn("Could not delete account for customerId={}: {}", id, e.getMessage());
        }

        publishNotification(customer.getEmail(), "Account Deletion Confirmation",
                buildDeletionMessage(accountDTO));

        customerRepository.deleteCustomerById(id);
        log.info("Customer deleted: id={}", id);
        return buildResponse(CustomerUtils.CUSTOMER_DELETION_CODE, CustomerUtils.CUSTOMER_DELETION_MESSAGE, accountDTO, customer);
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────

    public BankDto deleteCustomerFallback(Long id, Exception ex) {
        log.error("AccountService circuit breaker triggered during customer deletion: {}", ex.getMessage());
        throw new BusinessRuleException("Account service is unavailable. Customer deletion paused. Retry later.");
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void publishNotification(String receiver, String subject, String body) {
        // Fire-and-forget via async publisher — never blocks the HTTP thread
        notificationPublisher.publish(EXCHANGE_NAME, JSON_ROUTING_KEY, receiver, subject, body);
    }

    private String buildWelcomeMessage(Customer customer) {
        return String.format("Dear %s %s,%n%nWelcome to Our Bank! Your account has been registered.%n%nCode: %s%nMessage: %s",
                customer.getFirstName(), customer.getLastName(),
                CustomerUtils.CUSTOMER_CREATION_CODE, CustomerUtils.CUSTOMER_CREATION_MESSAGE);
    }

    private String buildDeletionMessage(AccountDTO accountDTO) {
        StringBuilder sb = new StringBuilder(String.format("Code: %s%nMessage: %s%n",
                CustomerUtils.CUSTOMER_DELETION_CODE, CustomerUtils.CUSTOMER_DELETION_MESSAGE));
        if (accountDTO != null && accountDTO.getAccountNumber() != null) {
            sb.append(String.format("Closed Account: %s%nFinal Balance: %s",
                    accountDTO.getAccountNumber(), accountDTO.getBalance()));
        }
        return sb.toString();
    }

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

    private CustomerDTO convertToDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .gender(customer.getGender())
                .address(customer.getAddress())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .createdAt(customer.getCreatedAt())
                .modifiedAt(customer.getModifiedAt())
                .build();
    }
}
