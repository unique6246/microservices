package com.example.notificationservice.service;

import com.example.notificationservice.Repo.NotificationRepository;
import com.example.notificationservice.dto.NotificationDTO;
import com.example.notificationservice.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationRepository notificationRepository;

    public void sendNotification(NotificationDTO notificationDTO) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notificationDTO.getTo());
        message.setSubject(notificationDTO.getSubject());
        message.setText(notificationDTO.getBody());
        mailSender.send(message);

        saveNotification(notificationDTO);
    }

    private void saveNotification(NotificationDTO notificationDTO) {
        Notification notification = new Notification();
        notification.setTo(notificationDTO.getTo());
        notification.setSubject(notificationDTO.getSubject());
        notification.setBody(notificationDTO.getBody());
        notificationRepository.save(notification);
    }
}
