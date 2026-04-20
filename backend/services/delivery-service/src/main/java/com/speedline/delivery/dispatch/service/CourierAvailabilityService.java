package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DISP-101: reads courier availability from Redis.
 *
 * <p>Reuses the key layout written by {@code location-service} (see {@code CourierPositionRedisService}):
 * <ul>
 *   <li>{@code courier:{id}:position} — STRING JSON (lat/lng/accuracy/...)</li>
 *   <li>{@code courier:{id}:isOnline} — STRING "true"/"false"</li>
 *   <li>{@code courier:{id}:zone} — STRING zone id (optional; if missing, courier is included in every zone)</li>
 *   <li>{@code courier:{id}:status} — STRING CourierStatus enum name (optional; defaults to IDLE)</li>
 * </ul>
 *
 * <p>Day-1 falls back to SCAN over {@code courier:*:isOnline}. A per-zone index
 * ({@code dispatch:couriers:zone:{zoneId}}) is the planned optimization (DISP-102).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourierAvailabilityService {

    private static final String ZONE_INDEX_PREFIX = "dispatch:couriers:zone:";
    private static final String COURIER_PREFIX = "courier:";
    private static final String POSITION_SUFFIX = ":position";
    private static final String ONLINE_SUFFIX = ":isOnline";
    private static final String ZONE_SUFFIX = ":zone";
    private static final String STATUS_SUFFIX = ":status";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Return all couriers available to dispatch for the given zone (IDLE or PRE_ASSIGNABLE, online). */
    public List<AvailableCourier> findAvailableByZone(Long zoneId) {
        if (zoneId == null) return List.of();

        Set<String> courierIds = readZoneIndex(zoneId);
        if (courierIds.isEmpty()) {
            courierIds = scanAllOnlineCouriers();
        }
        if (courierIds.isEmpty()) return List.of();

        final List<AvailableCourier> out = new ArrayList<>(courierIds.size());
        for (String courierId : courierIds) {
            AvailableCourier c = toAvailableCourier(courierId, zoneId);
            if (c != null) out.add(c);
        }
        return out;
    }

    private Set<String> readZoneIndex(Long zoneId) {
        Set<String> members = redisTemplate.opsForSet().members(ZONE_INDEX_PREFIX + zoneId);
        return members == null ? Set.of() : members;
    }

    private Set<String> scanAllOnlineCouriers() {
        final Set<String> ids = new HashSet<>();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (var cursor = connection.scan(ScanOptions.scanOptions()
                    .match(COURIER_PREFIX + "*" + ONLINE_SUFFIX).count(1000).build())) {
                cursor.forEachRemaining(raw -> {
                    String key = new String(raw);
                    String id = key.substring(COURIER_PREFIX.length(), key.length() - ONLINE_SUFFIX.length());
                    ids.add(id);
                });
            } catch (Exception e) {
                log.error("SCAN courier:*:isOnline failed: {}", e.getMessage(), e);
            }
            return null;
        });
        return ids;
    }

    private AvailableCourier toAvailableCourier(String courierId, Long requestedZoneId) {
        if (!"true".equalsIgnoreCase(redisTemplate.opsForValue().get(COURIER_PREFIX + courierId + ONLINE_SUFFIX))) {
            return null;
        }
        final String courierZone = redisTemplate.opsForValue().get(COURIER_PREFIX + courierId + ZONE_SUFFIX);
        if (courierZone != null && !courierZone.isBlank() && !courierZone.equals(String.valueOf(requestedZoneId))) {
            return null;
        }

        final CourierStatus status = readStatus(courierId);
        if (status == CourierStatus.ON_DELIVERY) {
            return null;
        }

        final Long id = parseLongOrNull(courierId);
        if (id == null) return null;

        final String positionJson = redisTemplate.opsForValue().get(COURIER_PREFIX + courierId + POSITION_SUFFIX);
        Double lat = null, lon = null;
        if (positionJson != null && !positionJson.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(positionJson);
                lat = node.has("lat") ? node.get("lat").asDouble() : null;
                lon = node.has("lng") ? node.get("lng").asDouble() : (node.has("lon") ? node.get("lon").asDouble() : null);
            } catch (Exception e) {
                log.warn("Invalid position JSON for courier {}: {}", courierId, e.getMessage());
            }
        }

        return AvailableCourier.builder()
                .id(id)
                .zoneId(requestedZoneId)
                .type(CourierType.INTERNAL)
                .status(status)
                .lat(lat)
                .lon(lon)
                .rating(5.0)
                .currentLoad(0)
                .maxCapacity(1)
                .build();
    }

    private CourierStatus readStatus(String courierId) {
        final String raw = redisTemplate.opsForValue().get(COURIER_PREFIX + courierId + STATUS_SUFFIX);
        if (raw == null || raw.isBlank()) return CourierStatus.IDLE;
        try {
            return CourierStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CourierStatus.IDLE;
        }
    }

    private static Long parseLongOrNull(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
