package com.example.transactionservice.dto;

import lombok.Data;

@Data
public class TransferDTO {
    private String fromAccount;
    private String toAccount;
    private double amount;


}
