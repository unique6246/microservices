package com.example.transactionservice.dto;

import lombok.Data;

@Data
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private String accountType;
    private double balance;
    private long customerId;
    // Getters and Setters
}
