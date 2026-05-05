package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationDTO;
import com.example.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Email notification dispatch")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @Operation(summary = "Send a notification email synchronously")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request or email format"),
            @ApiResponse(responseCode = "500", description = "Mail delivery failed")
    })
    public ResponseEntity<String> sendNotification(@Valid @RequestBody NotificationDTO notificationDTO) {
        log.info("Sync notification request for receiver={}", notificationDTO.getReceiver());
        return ResponseEntity.ok(notificationService.sendNotification(notificationDTO));
    }
}
