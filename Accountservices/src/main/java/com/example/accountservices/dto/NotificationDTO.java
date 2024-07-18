package com.example.accountservices.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private String receiver;
    private String subject;
    private String body;


}
