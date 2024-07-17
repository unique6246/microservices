package com.example.accountservices.dto;

import lombok.Data;

@Data
public class UpdateBalanceDTO {
    private String accountNumber;
    private double amount;


}
