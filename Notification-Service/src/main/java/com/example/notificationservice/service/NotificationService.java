package com.example.notificationservice.service;

import com.example.notificationservice.Repo.NotificationRepository;
import com.example.notificationservice.dto.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("$spring.mail.username")
    private String fromEmail;

    @Autowired
    private NotificationRepository notificationRepository;

    public void sendNotification(NotificationDTO notificationDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(notificationDTO.getReceiver());
        message.setSubject(notificationDTO.getSubject());
        message.setText(notificationDTO.getBody());
        mailSender.send(message);

        saveNotification(notificationDTO);
    }

    private void saveNotification(NotificationDTO notificationDTO) {
        Notification notification = new Notification();
        notification.setReceiver(notificationDTO.getReceiver());
        notification.setSubject(notificationDTO.getSubject());
        notification.setBody(notificationDTO.getBody());
        notificationRepository.save(notification);
    }
}
