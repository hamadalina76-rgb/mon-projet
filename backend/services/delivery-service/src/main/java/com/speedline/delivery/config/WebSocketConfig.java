package com.speedline.delivery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.security.JwtUtil;
import com.speedline.delivery.websocket.TrackingBroadcastWebSocketHandler;
import com.speedline.delivery.websocket.TrackingWebSocketHandler;
import com.speedline.delivery.websocket.WebSocketAuthInterceptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for delivery-service.
 * Expose:
 * - /ws/location : flux entrant des positions livreur
 * - /ws/tracking/{orderId} : canal de diffusion (à implémenter dans un second temps)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public WebSocketConfig(StringRedisTemplate stringRedisTemplate,
                           JwtUtil jwtUtil,
                           ObjectMapper objectMapper,
                           ApplicationEventPublisher eventPublisher) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var authInterceptor = new WebSocketAuthInterceptor(jwtUtil);

        registry.addHandler(locationWebSocketHandler(), "/ws/location")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(
                        "http://localhost:4300",
                        "http://localhost:4200",
                        "http://localhost:4201",
                        "http://localhost:4202"
                );

        // Endpoint de tracking client : handler minimal pour l’instant.
        registry.addHandler(trackingWebSocketHandler(), "/ws/tracking/{orderId}")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(
                        "http://localhost:4300",
                        "http://localhost:4200",
                        "http://localhost:4201",
                        "http://localhost:4202"
                );
    }

    @Bean
    public WebSocketHandler locationWebSocketHandler() {
        return new TrackingWebSocketHandler(stringRedisTemplate, objectMapper, eventPublisher);
    }

    @Bean
    public WebSocketHandler trackingWebSocketHandler() {
        return new TrackingBroadcastWebSocketHandler();
    }
}


