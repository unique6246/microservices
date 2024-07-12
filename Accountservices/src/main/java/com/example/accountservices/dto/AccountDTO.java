package com.example.accountservices.dto;

import lombok.Data;

@Data
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private String accountType;
    private double balance;

    // Getters and Setters
}
