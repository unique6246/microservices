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

    //all customers
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    //customer by id
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        return customer != null ? convertToDTO(customer) : null;
    }

    //customer creation
    public String  createCustomer(CustomerDTO customerDTO) {

        //customer already exists
        if (customerRepository.existsCustomerByEmail(customerDTO.getEmail())) {
            return "Customer with email " + customerDTO.getEmail() + " already exists use different email.";
        }

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
        return "Customer created successfully";
    }

    //customer deletion
    public String deleteCustomer(Long id) {
        if(!customerRepository.existsById(id)) {
            return "Customer with id " + id + " does not exist.";
        }
        customerRepository.deleteById(id);
        return "Customer deleted successfully";
    }

    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName(customer.getName());
        customerDTO.setEmail(customer.getEmail());
        return customerDTO;
    }
}
