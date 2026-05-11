package com.example.transactionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String address;
    private String phoneNumber;
    private String email;

    // ── Audit ──────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
