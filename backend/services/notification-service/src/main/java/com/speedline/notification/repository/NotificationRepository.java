package com.speedline.notification.repository;

import com.speedline.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour Notification (MongoDB)
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Trouver les notifications d'un utilisateur
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Trouver les notifications non lues d'un utilisateur
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Compter les notifications non lues
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Trouver les notifications par type
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, 
                                                                 com.speedline.notification.domain.NotificationType type, 
                                                                 Pageable pageable);

    /**
     * Trouver les notifications par canal
     */
    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(Long userId,
                                                                    com.speedline.notification.domain.NotificationChannel channel,
                                                                    Pageable pageable);

    /**
     * Trouver une notification par ID
     */
    java.util.Optional<Notification> findById(String id);

    /**
     * Supprimer les anciennes notifications (nettoyage)
     */
    void deleteByCreatedAtBefore(LocalDateTime date);

    /**
     * Trouver les notifications non envoyées
     */
    List<Notification> findByIsSentFalse();

    /**
     * Trouver les notifications dans une période
     */
    List<Notification> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
