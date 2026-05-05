package com.example.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification request payload")
public class NotificationDTO {

    @NotBlank(message = "Receiver email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "user@example.com")
    private String receiver;

    @NotBlank(message = "Subject is required")
    @Schema(example = "Transaction Alert")
    private String subject;

    @NotBlank(message = "Body is required")
    @Schema(example = "Your account has been credited with $500")
    private String body;
}
