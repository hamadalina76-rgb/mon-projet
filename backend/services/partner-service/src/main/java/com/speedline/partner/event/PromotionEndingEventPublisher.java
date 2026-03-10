package com.speedline.partner.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes "promotion ending in 3 days" events to GCP Pub/Sub (topic: partner-promotion-ending).
 * Consumed by notification-service to send the partner an in-app notification.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEndingEventPublisher {

    private static final String TOPIC = "partner-promotion-ending";

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    /** daysLeft = 3 for "in 3 days", 1 for "tomorrow". */
    @Async
    public void publishPromotionEnding(Long partnerId, Long partnerUserId, Long productId,
                                       String productName, String promotionLabel, LocalDate promotionEndDate, int daysLeft) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "PROMOTION_ENDING_SOON");
        payload.put("daysLeft", daysLeft);
        payload.put("partnerId", partnerId);
        payload.put("partnerUserId", partnerUserId != null ? partnerUserId : 0L);
        payload.put("productId", productId);
        payload.put("productName", productName != null ? productName : "");
        payload.put("promotionLabel", promotionLabel != null ? promotionLabel : "");
        payload.put("promotionEndDate", promotionEndDate != null ? promotionEndDate.toString() : "");
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.debug("Publishing promotion-ending event to {}: {}", TOPIC, jsonPayload);
            pubSubTemplate.publish(TOPIC, jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish promotion-ending event to Pub/Sub: {}", e.getMessage(), e);
        }
    }
}
