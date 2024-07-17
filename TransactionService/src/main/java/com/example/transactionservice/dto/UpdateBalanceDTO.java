package com.example.transactionservice.dto;

import lombok.Data;

@Data
public class UpdateBalanceDTO {
    private String accountNumber;
    private double amount;


}
