package com.speedline.promotion.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.speedline.promotion.dto.PromotionDto;
import com.speedline.promotion.dto.ValidatePromotionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEventPublisher {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    public void publishPromotionCreated(PromotionDto dto) {
        publish("promotion-created", Map.of(
            "eventType", "PROMOTION_CREATED",
            "promotionId", dto.id(),
            "code", dto.code(),
            "name", dto.name()
        ));
    }

    public void publishPromotionApplied(Long promotionId, String code, Long userId, Long orderId,
                                         ValidatePromotionResponse response) {
        publish("promotion-applied", Map.of(
            "eventType", "PROMOTION_APPLIED",
            "promotionId", promotionId,
            "code", code,
            "userId", userId,
            "orderId", orderId,
            "discountAmount", response.discountAmount()
        ));
    }

    public void publishPromotionRevoked(Long promotionId, String code, Long userId, Long orderId) {
        publish("promotion-revoked", Map.of(
            "eventType", "PROMOTION_REVOKED",
            "promotionId", promotionId,
            "code", code,
            "userId", userId,
            "orderId", orderId
        ));
    }

    public void publishPromotionExpired(Long promotionId, String code) {
        publish("promotion-expired", Map.of(
            "eventType", "PROMOTION_EXPIRED",
            "promotionId", promotionId,
            "code", code
        ));
    }

    private void publish(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            pubSubTemplate.publish(topic, json);
            log.debug("Published event to topic '{}': {}", topic, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic '{}': {}", topic, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish event to topic '{}': {}", topic, e.getMessage());
        }
    }
}
