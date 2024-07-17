package com.example.coustomerservices.controller;

import com.example.coustomerservices.dto.CustomerDTO;
import com.example.coustomerservices.dto.NotificationDTO;
import com.example.coustomerservices.feignconfig.NotificationServiceClient;
import com.example.coustomerservices.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        CustomerDTO customerDTO = customerService.getCustomerById(id);
        return customerDTO != null ? ResponseEntity.ok(customerDTO) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<String> createCustomer(@RequestBody CustomerDTO customerDTO) {
        customerService.createCustomer(customerDTO);

        // Send email notification
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setTo(customerDTO.getEmail());
        notificationDTO.setSubject("Welcome to our Bank");
        notificationDTO.setBody("Dear " + customerDTO.getName() + ",\n\nYour account has been created successfully.");
        notificationServiceClient.sendNotification(notificationDTO);

        return ResponseEntity.ok("Customer created successfully\n" + customerDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok("Customer deleted successfully");
    }
}
