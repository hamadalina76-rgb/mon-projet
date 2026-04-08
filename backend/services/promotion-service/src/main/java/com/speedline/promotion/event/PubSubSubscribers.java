package com.speedline.promotion.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.speedline.promotion.dto.RevokePromotionRequest;
import com.speedline.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Pub/Sub subscribers:
 * <ul>
 *   <li>order-cancelled → revoke the promotion</li>
 *   <li>user-registered → assign welcome promo (if configured)</li>
 * </ul>
 * Idempotent: revoke() checks APPLIED status before acting.
 * Nack on failure → Pub/Sub will retry with backoff.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PubSubSubscribers {

    private final PubSubTemplate pubSubTemplate;
    private final PromotionService promotionService;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void startListeners() {
        subscribeOrderCancelled();
        subscribeUserRegistered();
    }

    private void subscribeOrderCancelled() {
        try {
            pubSubTemplate.subscribe("order-cancelled-promotion-sub", message -> {
                try {
                    String payload = message.getPubsubMessage().getData().toStringUtf8();
                    log.info("[PubSub] Received order-cancelled: {}", payload);

                    JsonNode json = objectMapper.readTree(payload);
                    String code = json.has("promoCode") ? json.get("promoCode").asText() : null;
                    Long userId = json.has("userId") ? json.get("userId").asLong() : null;
                    Long orderId = json.has("orderId") ? json.get("orderId").asLong() : null;

                    if (code != null && userId != null && orderId != null) {
                        promotionService.revoke(new RevokePromotionRequest(code, userId, orderId));
                        log.info("[PubSub] Promotion revoked for cancelled order: code={} order={}", code, orderId);
                    }
                    message.ack();
                } catch (Exception e) {
                    log.error("[PubSub] Error processing order-cancelled: {}", e.getMessage());
                    message.nack();
                }
            });
            log.info("[PubSub] Subscribed to order-cancelled-promotion-sub");
        } catch (Exception e) {
            log.warn("[PubSub] Could not subscribe to order-cancelled (non-fatal): {}", e.getMessage());
        }
    }

    private void subscribeUserRegistered() {
        try {
            pubSubTemplate.subscribe("user-registered-promotion-sub", message -> {
                try {
                    String payload = message.getPubsubMessage().getData().toStringUtf8();
                    log.info("[PubSub] Received user-registered: {}", payload);
                    // Welcome promo: future enhancement — auto-assign a first-order promo to new users
                    message.ack();
                } catch (Exception e) {
                    log.error("[PubSub] Error processing user-registered: {}", e.getMessage());
                    message.nack();
                }
            });
            log.info("[PubSub] Subscribed to user-registered-promotion-sub");
        } catch (Exception e) {
            log.warn("[PubSub] Could not subscribe to user-registered (non-fatal): {}", e.getMessage());
        }
    }
}
