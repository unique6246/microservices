package com.example.coustomerservices.service;

import com.example.coustomerservices.Repo.CustomerRepository;
import com.example.coustomerservices.dto.CustomerDTO;
import com.example.coustomerservices.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        return customer != null ? convertToDTO(customer) : null;
    }

    public void createCustomer(CustomerDTO customerDTO) {
        // Check if the customer already exists by email
        if (customerRepository.existsCustomerByEmail(customerDTO.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email " + customerDTO.getEmail() + " already exists use different email.");
        }

        // Create and save the new customer
        Customer customer = new Customer();
        customer.setName(customerDTO.getName());
        customer.setEmail(customerDTO.getEmail());
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
