package com.example.transactionservice.dto;

import lombok.Data;

@Data
public class TransactionDTO {
    private Long id;
    private String accountNumber;
    private double amount;
    private String transactionType;
    private Long accountId;

    // Getters and Setters
}
