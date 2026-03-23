package com.speedline.partner.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes partner events to GCP Pub/Sub.
 * Uses async to avoid blocking the main request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerEventPublisher {

    private static final String TOPIC = "partner-events";
    
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publish(PartnerEvent event) {
        log.info("========== PUBLISHING EVENT TO PUB/SUB ==========");
        log.info("Event type: {}, Partner ID: {}, User ID: {}", event.getEventType(), event.getPartnerId(), event.getUserId());

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", event.getEventType().name());
            payload.put("partnerId", event.getPartnerId());
            payload.put("userId", event.getUserId() != null ? event.getUserId() : 0L);
            payload.put("businessName", event.getBusinessName() != null ? event.getBusinessName() : "");
            payload.put("brandName", event.getBrandName() != null ? event.getBrandName() : "");
            payload.put("email", event.getEmail() != null ? event.getEmail() : "");
            payload.put("status", event.getStatus() != null ? event.getStatus() : "");
            payload.put("reason", event.getReason() != null ? event.getReason() : "");
            payload.put("productId", event.getProductId() != null ? event.getProductId() : 0L);
            payload.put("productName", event.getProductName() != null ? event.getProductName() : "");
            payload.put("newModerationStatus", event.getNewModerationStatus() != null ? event.getNewModerationStatus() : "");
            payload.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : "");

            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.info("JSON Payload: {}", jsonPayload);
            log.info("Publishing to topic: {}", TOPIC);
            
            pubSubTemplate.publish(TOPIC, jsonPayload);
            
            log.info("========== EVENT PUBLISHED SUCCESSFULLY ==========");
        } catch (Exception e) {
            log.error("========== ERROR: Failed to publish event to Pub/Sub ==========");
            log.error("Error message: {}", e.getMessage(), e);
        }
    }
}
