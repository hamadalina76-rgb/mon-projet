package com.speedline.user.client;

import com.speedline.user.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign Client pour envoyer des emails via le notification-service (admin -> client).
 */
@FeignClient(name = "notification-service", configuration = FeignConfig.class)
public interface NotificationServiceClient {

    /**
     * Envoyer un email à un utilisateur (endpoint admin send-to-user).
     */
    @PostMapping("/notifications/send-to-user")
    void sendEmailToUser(@RequestBody SendEmailToUserRequest request);

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
}
