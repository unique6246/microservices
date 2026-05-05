package com.example.coustomerservices.config;
import com.example.coustomerservices.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
/**
 * Fire-and-forget async RabbitMQ publisher.
 * Prevents blocking RabbitMQ operations from delaying HTTP responses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;
    @Async("notificationExecutor")
    public void publish(String exchange, String routingKey,
                        String receiver, String subject, String body) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey,
                    NotificationDTO.builder()
                            .receiver(receiver)
                            .subject(subject)
                            .body(body)
                            .build());
            log.debug("Notification published async: receiver={}", receiver);
        } catch (Exception e) {
            log.error("Async notification publish failed for receiver={}: {}", receiver, e.getMessage());
        }
    }
}