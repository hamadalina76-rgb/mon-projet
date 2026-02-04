package com.speedline.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entité Notification - MongoDB Document
 */
@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private NotificationType type;

    private String title;

    private String message;

    private Map<String, Object> data;

    @Builder.Default
    private Boolean isRead = false;

    private NotificationChannel channel;

    private String actionUrl;

    private String imageUrl;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    private LocalDateTime sentAt;

    @Builder.Default
    private Boolean isSent = false;

    private String errorMessage;
}
