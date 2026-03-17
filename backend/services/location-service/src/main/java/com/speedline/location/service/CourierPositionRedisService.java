package com.speedline.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.location.dto.CourierPositionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierPositionRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record RedisPositionPayload(
            double lat,
            double lng,
            Double accuracy,
            Double speed,
            Double heading,
            Integer batteryLevel,
            String timestamp
    ) {
    }

    public Optional<CourierPositionDto> getPosition(String courierId) {
        String key = "courier:%s:position".formatted(courierId);
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            RedisPositionPayload payload = objectMapper.readValue(raw, RedisPositionPayload.class);
            return Optional.of(new CourierPositionDto(
                    courierId,
                    payload.lat(),
                    payload.lng(),
                    payload.speed(),
                    payload.heading(),
                    payload.timestamp()
            ));
        } catch (Exception e) {
            log.error("Failed to parse Redis position for courier {}: {}", courierId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<CourierPositionDto> getAllOnline() {
        Set<String> onlineKeys = scanKeys("courier:*:isOnline");
        if (onlineKeys.isEmpty()) {
            return List.of();
        }
        return onlineKeys.stream()
                .map(key -> {
                    String prefix = "courier:";
                    String suffix = ":isOnline";
                    if (!key.startsWith(prefix) || !key.endsWith(suffix)) {
                        return null;
                    }
                    String courierId = key.substring(prefix.length(), key.length() - suffix.length());
                    return getPosition(courierId).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            try (var cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(1000)
                            .build())) {
                cursor.forEachRemaining(item -> keys.add(new String(item)));
            } catch (Exception e) {
                log.error("Error scanning Redis with pattern {}: {}", pattern, e.getMessage());
            }
            return null;
        });
        return keys;
    }
}

