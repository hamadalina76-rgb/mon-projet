package com.speedline.user.client;

import com.speedline.user.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign Client pour le notification-service (emails, notifications in-app, broadcast admin).
 */
@FeignClient(name = "notification-service", configuration = FeignConfig.class)
public interface NotificationServiceClient {

    /**
     * Envoyer un email à un utilisateur (endpoint admin send-to-user).
     */
    @PostMapping("/notifications/send-to-user")
    void sendEmailToUser(@RequestBody SendEmailToUserRequest request);

    /**
     * Broadcast aux admins (nouveau livreur, etc.) : crée une notification userId=0 et push WebSocket.
     */
    @PostMapping("/notifications/admin/broadcast")
    void sendAdminBroadcast(@RequestBody AdminBroadcastRequest request);

    /**
     * Envoyer une notification in-app à un utilisateur (ex: livreur approuvé / rejeté).
     */
    @PostMapping("/notifications/send")
    void sendNotification(@RequestBody SendNotificationRequest request);

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class SendEmailToUserRequest {
        private String email;
        private String subject;
        private String body;
        private String templateName;
        private Map<String, Object> variables;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class AdminBroadcastRequest {
        private String type;
        private String title;
        private String message;
        private Map<String, Object> data;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class SendNotificationRequest {
        private Long userId;
        private String type;
        private String title;
        private String message;
        private Map<String, Object> data;
        private String channel;
    }
}
