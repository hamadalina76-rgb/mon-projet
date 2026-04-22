package com.speedline.delivery.dispatch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Runtime override for per-zone dispatch mode (Redis). Falls back to YAML via {@link DispatchZoneConfig}
 * when no override is set.
 */
@Component
@RequiredArgsConstructor
public class DispatchZoneModeStore {

    static String key(Long zoneId) {
        return "dispatch:zone:" + zoneId + ":mode";
    }

    private final StringRedisTemplate redisTemplate;

    public Optional<DispatchMode> getOverride(Long zoneId) {
        if (zoneId == null) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(key(zoneId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(DispatchMode.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public void setOverride(Long zoneId, DispatchMode mode) {
        if (zoneId == null || mode == null) {
            return;
        }
        redisTemplate.opsForValue().set(key(zoneId), mode.name());
    }

    public void clearOverride(Long zoneId) {
        if (zoneId == null) {
            return;
        }
        redisTemplate.delete(key(zoneId));
    }
}
