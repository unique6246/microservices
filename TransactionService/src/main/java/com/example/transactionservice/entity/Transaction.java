package com.example.transactionservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accountNumber;
    private double amount;
    private String transactionType; // "credit" or "debit"

    // Getters and Setters
}
