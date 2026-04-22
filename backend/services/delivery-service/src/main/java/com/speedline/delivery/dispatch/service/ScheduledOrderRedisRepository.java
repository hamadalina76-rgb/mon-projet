package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ScheduledOrderRedisRepository {

    static final String ZONE_KEY = "dispatch:scheduled:zone:%d";
    static final String ORDER_KEY = "dispatch:scheduled:order:%d";
    static final String ORDER_PAYLOAD_KEY = "dispatch:scheduled:payload:%d";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DispatchProperties properties;

    public boolean add(PendingOrder order) {
        if (order == null || order.getId() == null || order.getZoneId() == null || order.getScheduledDeliveryAt() == null) {
            return false;
        }

        String orderIndexKey = ORDER_KEY.formatted(order.getId());
        Duration ttl = Duration.ofHours(properties.getScheduledOrders().getTtlHours());
        Boolean inserted = redisTemplate.opsForValue().setIfAbsent(orderIndexKey, String.valueOf(order.getZoneId()), ttl);
        if (!Boolean.TRUE.equals(inserted)) {
            return false;
        }

        try {
            String payload = objectMapper.writeValueAsString(order);
            redisTemplate.opsForValue().set(ORDER_PAYLOAD_KEY.formatted(order.getId()), payload, ttl);
            redisTemplate.opsForZSet().add(ZONE_KEY.formatted(order.getZoneId()), payload, order.getScheduledDeliveryAt().toEpochMilli());
            redisTemplate.expire(ZONE_KEY.formatted(order.getZoneId()), ttl);
            return true;
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(orderIndexKey);
            log.error("Failed to serialize scheduled order orderId={}: {}", order.getId(), ex.getMessage(), ex);
            return false;
        }
    }

    public List<PendingOrder> findDueWithin(Long zoneId, Instant now, long windowSeconds) {
        if (zoneId == null || now == null) {
            return List.of();
        }
        long from = now.toEpochMilli();
        long to = now.plusSeconds(windowSeconds).toEpochMilli();
        return findByScoreRange(zoneId, from, to);
    }

    public List<PendingOrder> findByScoreRange(Long zoneId, long fromMillis, long toMillis) {
        if (zoneId == null) {
            return List.of();
        }
        Set<String> values = redisTemplate.opsForZSet().rangeByScore(ZONE_KEY.formatted(zoneId), fromMillis, toMillis);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<PendingOrder> out = new ArrayList<>(values.size());
        for (String payload : values) {
            try {
                out.add(objectMapper.readValue(payload, PendingOrder.class));
            } catch (Exception ex) {
                log.warn("Invalid scheduled order payload zoneId={}: {}", zoneId, ex.getMessage());
            }
        }
        return out;
    }

    public void remove(Long orderId) {
        if (orderId == null) {
            return;
        }
        String zoneId = redisTemplate.opsForValue().get(ORDER_KEY.formatted(orderId));
        String payload = redisTemplate.opsForValue().get(ORDER_PAYLOAD_KEY.formatted(orderId));
        if (zoneId != null && payload != null) {
            redisTemplate.opsForZSet().remove(ZONE_KEY.formatted(Long.valueOf(zoneId)), payload);
        }
        redisTemplate.delete(ORDER_KEY.formatted(orderId));
        redisTemplate.delete(ORDER_PAYLOAD_KEY.formatted(orderId));
    }

    public long countByZone(Long zoneId) {
        if (zoneId == null) {
            return 0L;
        }
        Long count = redisTemplate.opsForZSet().zCard(ZONE_KEY.formatted(zoneId));
        return count == null ? 0L : count;
    }
}
