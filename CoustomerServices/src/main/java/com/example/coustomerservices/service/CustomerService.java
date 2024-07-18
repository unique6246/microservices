package com.example.coustomerservices.service;

import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.dto.CustomerDTO;
import com.example.coustomerservices.dto.NotificationDTO;
import com.example.coustomerservices.entity.Customer;
import com.example.coustomerservices.feignconfig.NotificationServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        return customer != null ? convertToDTO(customer) : null;
    }

    public void createCustomer(CustomerDTO customerDTO) {

        // Create and save the new customer
        Customer customer = new Customer();
        customer.setName(customerDTO.getName());
        customer.setEmail(customerDTO.getEmail());

        // Send email notification
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setReceiver(customerDTO.getEmail());
        notificationDTO.setSubject("Welcome to our Bank");
        notificationDTO.setBody("Dear " + customerDTO.getName() + ",\n\nYour account has been created successfully.");
        notificationServiceClient.sendNotification(notificationDTO);

        customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName(customer.getName());
        customerDTO.setEmail(customer.getEmail());
        return customerDTO;
    }
}
