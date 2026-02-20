package com.speedline.notification.dto;

import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for sending a notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, Object> data;
    private NotificationChannel channel;
}
