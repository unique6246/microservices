package com.example.coustomerservices.controller;

import com.example.coustomerservices.dto.BankDto;
import com.example.coustomerservices.dto.CustomerDTO;
import com.example.coustomerservices.service.CustomerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer profile and onboarding management")
public class CustomerController {

    private final CustomerServiceImpl customerService;

    @GetMapping
    @Operation(summary = "List all customers")
    @ApiResponse(responseCode = "200", description = "Customers retrieved")
    public ResponseEntity<Page<CustomerDTO>> getAllCustomers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Fetching all customers, page={}", pageable.getPageNumber());
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerDTO> getCustomerById(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        log.info("Fetching customerId={}", id);
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Customer already exists")
    })
    public ResponseEntity<BankDto> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        log.info("Registering customer email={}", customerDTO.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createAccount(customerDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer and their associated account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer deleted"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<BankDto> deleteCustomer(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        log.info("Deleting customerId={}", id);
        return ResponseEntity.ok(customerService.deleteCustomer(id));
    }
}