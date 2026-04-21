package com.speedline.delivery.dispatch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PartnerDelayTrackerRepository {

    private static final String ACTIVE_DELAY_KEY = "dispatch:partner-delay:active";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void upsert(Map<String, Object> event) {
        final Long orderId = parseLong(event.get("orderId"));
        if (orderId == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.opsForHash().put(ACTIVE_DELAY_KEY, String.valueOf(orderId), payload);
        } catch (Exception ex) {
            log.warn("Unable to store partner delay event orderId={}: {}", orderId, ex.getMessage());
        }
    }

    public List<Map<String, Object>> findAll() {
        final Map<Object, Object> entries = redisTemplate.opsForHash().entries(ACTIVE_DELAY_KEY);
        final List<Map<String, Object>> out = new ArrayList<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            try {
                Map<String, Object> payload = objectMapper.readValue(
                        String.valueOf(entry.getValue()),
                        new TypeReference<>() {});
                out.add(payload);
            } catch (Exception ex) {
                log.warn("Unable to parse active partner delay payload field={}: {}", entry.getKey(), ex.getMessage());
            }
        }
        return out;
    }

    public void remove(Long orderId) {
        if (orderId == null) {
            return;
        }
        redisTemplate.opsForHash().delete(ACTIVE_DELAY_KEY, String.valueOf(orderId));
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
