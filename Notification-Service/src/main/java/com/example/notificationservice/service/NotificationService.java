package com.example.notificationservice.service;

import com.example.notificationservice.Repo.NotificationRepository;
import com.example.notificationservice.dto.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * Primary consumer: processes notification events from RabbitMQ asynchronously.
     * On processing failure the message is negatively acknowledged and routed to DLQ.
     */
    @RabbitListener(queues = "${account.queue.json.name}", ackMode = "AUTO")
    public void handleNotificationEvent(NotificationDTO notificationDTO) {
        log.info("Received async notification event for receiver={}", notificationDTO.getReceiver());
        try {
            String result = sendNotification(notificationDTO);
            log.info("Async notification processed: receiver={}, result={}", notificationDTO.getReceiver(), result);
        } catch (Exception e) {
            log.error("Failed to process async notification for receiver={}: {}",
                    notificationDTO.getReceiver(), e.getMessage());
            throw e; // re-throw so Spring AMQP routes to DLQ
        }
    }

    /**
     * Synchronous endpoint: called directly via REST POST /api/v1/notifications/send
     */
    public String sendNotification(NotificationDTO notificationDTO) {
        validateNotification(notificationDTO);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notificationDTO.getReceiver());
            message.setSubject(notificationDTO.getSubject());
            message.setText(notificationDTO.getBody());
            mailSender.send(message);
            log.info("Email sent to={}", notificationDTO.getReceiver());
        } catch (Exception e) {
            log.error("Mail delivery failed for receiver={}: {}", notificationDTO.getReceiver(), e.getMessage());
            // Persist with FAILED status so it's traceable
            saveNotification(notificationDTO, "FAILED");
            throw e;
        }

        saveNotification(notificationDTO, "SENT");
        return "Notification sent successfully";
    }

    private void validateNotification(NotificationDTO dto) {
        if (dto.getReceiver() == null || dto.getReceiver().isBlank()) {
            throw new IllegalArgumentException("Receiver email is required");
        }
        Matcher matcher = EMAIL_PATTERN.matcher(dto.getReceiver());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid email format: " + dto.getReceiver());
        }
        if (dto.getSubject() == null || dto.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (dto.getBody() == null || dto.getBody().isBlank()) {
            throw new IllegalArgumentException("Body is required");
        }
    }

    private void saveNotification(NotificationDTO dto, String status) {
        notificationRepository.save(Notification.builder()
                .receiver(dto.getReceiver())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .status(status)
                .build());
    }
}
