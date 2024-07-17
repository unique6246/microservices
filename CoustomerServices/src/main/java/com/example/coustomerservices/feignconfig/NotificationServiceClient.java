package com.example.coustomerservices.feignconfig;

import com.example.coustomerservices.dto.NotificationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("notification-service")
public interface NotificationServiceClient {

    @PostMapping("/notifications/send")
    void sendNotification(@RequestBody NotificationDTO notificationDTO);
}
