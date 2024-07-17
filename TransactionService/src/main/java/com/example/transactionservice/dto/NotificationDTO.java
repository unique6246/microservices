package com.example.transactionservice.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private String to;
    private String subject;
    private String body;


}
