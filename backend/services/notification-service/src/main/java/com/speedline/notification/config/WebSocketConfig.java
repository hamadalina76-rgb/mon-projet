package com.speedline.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP protocol
 * Clients connect via /ws/notifications and subscribe to /topic/* channels
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
            .setAllowedOrigins(
                "https://admin-panel-392205979525.europe-west1.run.app",
                "https://partner-dashboard-392205979525.europe-west1.run.app",
                "https://courier-app-392205979525.europe-west1.run.app",
                "https://customer-app-392205979525.europe-west1.run.app",
                "http://localhost:4200",
                "http://localhost:4201",
                "http://localhost:4202"
            )
                .withSockJS();
    }
}
