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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DISP-101: Redis-backed store for pending orders awaiting dispatch.
 *
 * <p>Layout:
 * <ul>
 *   <li>{@code dispatch:pending:zone:{zoneId}} — HASH, field=orderId, value=JSON(PendingOrder)</li>
 *   <li>{@code dispatch:pending:order:{orderId}} — STRING, value=zoneId (reverse index for O(1) removal)</li>
 * </ul>
 * Add is idempotent via HSETNX so duplicate {@code ORDER_CREATED} events (Pub/Sub at-least-once) are no-ops.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class PendingOrderRedisRepository {

    static final String ZONE_KEY_PREFIX = "dispatch:pending:zone:";
    static final String ORDER_INDEX_PREFIX = "dispatch:pending:order:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DispatchProperties properties;

    /**
     * Insert a pending order under its zone. No-op if the order is already present (idempotent).
     *
     * @return {@code true} if the order was newly inserted, {@code false} if already present
     */
    public boolean add(PendingOrder order) {
        if (order == null || order.getId() == null || order.getZoneId() == null) {
            log.warn("Refusing to enqueue pending order with null id/zoneId: {}", order);
            return false;
        }
        final String zoneKey = zoneKey(order.getZoneId());
        final String orderField = String.valueOf(order.getId());
        try {
            final String json = objectMapper.writeValueAsString(order);
            Boolean inserted = redisTemplate.opsForHash().putIfAbsent(zoneKey, orderField, json);

            final Duration ttl = Duration.ofHours(properties.getPending().getTtlHours());
            redisTemplate.expire(zoneKey, ttl);
            redisTemplate.opsForValue().set(orderIndexKey(order.getId()), String.valueOf(order.getZoneId()), ttl);

            if (Boolean.TRUE.equals(inserted)) {
                log.info("Pending order enqueued orderId={} zoneId={}", order.getId(), order.getZoneId());
                return true;
            }
            log.debug("Pending order already present, skipping orderId={} zoneId={}", order.getId(), order.getZoneId());
            return false;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PendingOrder id={}: {}", order.getId(), e.getMessage(), e);
            return false;
        }
    }

    /** Returns every pending order currently queued for {@code zoneId}. */
    public List<PendingOrder> findByZone(Long zoneId) {
        if (zoneId == null) return List.of();
        final Map<Object, Object> entries = redisTemplate.opsForHash().entries(zoneKey(zoneId));
        if (entries.isEmpty()) return List.of();

        final List<PendingOrder> out = new ArrayList<>(entries.size());
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            final String json = Objects.toString(e.getValue(), null);
            if (json == null) continue;
            try {
                out.add(objectMapper.readValue(json, PendingOrder.class));
            } catch (Exception ex) {
                log.error("Corrupted pending order payload zoneId={} field={}: {}", zoneId, e.getKey(), ex.getMessage());
            }
        }
        return out;
    }

    /** Remove the given order from Redis (both zone hash and reverse index). */
    public void remove(Long orderId) {
        if (orderId == null) return;
        final String indexKey = orderIndexKey(orderId);
        final String zoneId = redisTemplate.opsForValue().get(indexKey);
        if (zoneId != null) {
            redisTemplate.opsForHash().delete(zoneKey(Long.valueOf(zoneId)), String.valueOf(orderId));
        }
        redisTemplate.delete(indexKey);
    }

    public long countByZone(Long zoneId) {
        if (zoneId == null) return 0L;
        Long size = redisTemplate.opsForHash().size(zoneKey(zoneId));
        return size == null ? 0L : size;
    }

    static String zoneKey(Long zoneId) {
        return ZONE_KEY_PREFIX + zoneId;
    }

    static String orderIndexKey(Long orderId) {
        return ORDER_INDEX_PREFIX + orderId;
    }
}
