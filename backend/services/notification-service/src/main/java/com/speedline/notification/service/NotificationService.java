package com.speedline.notification.service;

import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service pour la gestion des notifications
 */
public interface NotificationService {

    /**
     * Envoyer une notification
     */
    void sendNotification(Long userId, NotificationType type, String title, 
                          String message, Map<String, Object> data, NotificationChannel channel);

    /**
     * Envoyer une notification push
     */
    void sendPushNotification(Long userId, String title, String message, Map<String, Object> data);

    /**
     * Envoyer un SMS
     */
    void sendSms(String phoneNumber, String message);

    /**
     * Envoyer un email
     */
    void sendEmail(String email, String subject, String templateName, Map<String, Object> variables);

    /**
     * Récupérer les notifications d'un utilisateur
     */
    Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable);

    /**
     * Marquer comme lu
     */
    void markAsRead(String notificationId);

    /**
     * Marquer toutes comme lues
     */
    void markAllAsRead(Long userId);

    /**
     * Compter les notifications non lues
     */
    long getUnreadCount(Long userId);

    /**
     * Enregistrer un token push
     */
    void registerPushToken(Long userId, String token, String deviceType, String deviceId);

    /**
     * Supprimer un token push
     */
    void removePushToken(String token);

    /**
     * DTO pour les notifications
     */
    record NotificationDTO(
            String id,
            Long userId,
            NotificationType type,
            String title,
            String message,
            Map<String, Object> data,
            Boolean isRead,
            NotificationChannel channel,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime readAt
    ) {}
}
