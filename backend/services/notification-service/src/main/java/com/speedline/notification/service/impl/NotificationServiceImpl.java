package com.speedline.notification.service.impl;

import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Implémentation du service de gestion des notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    // TODO: Injecter FCMService, SMSService, EmailService, PushTokenRepository

    @Override
    @Transactional
    public void sendNotification(Long userId, NotificationType type, String title,
                                String message, Map<String, Object> data, NotificationChannel channel) {
        // TODO: Implémenter l'envoi de notification
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void sendPushNotification(Long userId, String title, String message, Map<String, Object> data) {
        // TODO: Implémenter l'envoi de push notification
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void sendSms(String phoneNumber, String message) {
        // TODO: Implémenter l'envoi de SMS
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void sendEmail(String email, String subject, String templateName, Map<String, Object> variables) {
        // TODO: Implémenter l'envoi d'email
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        // TODO: Implémenter la récupération des notifications
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId) {
        // TODO: Implémenter le marquage comme lu
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        // TODO: Implémenter le marquage de toutes comme lues
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        // TODO: Implémenter le comptage des non lues
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void registerPushToken(Long userId, String token, String deviceType, String deviceId) {
        // TODO: Implémenter l'enregistrement du token push
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void removePushToken(String token) {
        // TODO: Implémenter la suppression du token push
        throw new UnsupportedOperationException("À implémenter");
    }
}
