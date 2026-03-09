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
 * Publishes partner product stock events to GCP Pub/Sub (topic: partner-product-stock).
 * Events: PRODUCT_LOW_STOCK, PRODUCT_OUT_OF_STOCK.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerStockEventPublisher {

    private static final String TOPIC = "partner-product-stock";

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publishLowStock(Long partnerId, Long partnerUserId, Long productId, String productName, int quantity, int threshold) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "PRODUCT_LOW_STOCK");
        payload.put("partnerId", partnerId);
        payload.put("partnerUserId", partnerUserId != null ? partnerUserId : 0L);
        payload.put("productId", productId);
        payload.put("productName", productName != null ? productName : "");
        payload.put("quantity", quantity);
        payload.put("threshold", threshold);
        publish(payload);
    }

    @Async
    public void publishOutOfStock(Long partnerId, Long partnerUserId, Long productId, String productName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "PRODUCT_OUT_OF_STOCK");
        payload.put("partnerId", partnerId);
        payload.put("partnerUserId", partnerUserId != null ? partnerUserId : 0L);
        payload.put("productId", productId);
        payload.put("productName", productName != null ? productName : "");
        publish(payload);
    }

    private void publish(Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.debug("Publishing stock event to {}: {}", TOPIC, jsonPayload);
            pubSubTemplate.publish(TOPIC, jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish stock event to Pub/Sub: {}", e.getMessage(), e);
        }
    }
}
