package com.speedline.delivery.matching.cost.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.service.DispatchConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDispatchConfigService implements DispatchConfigService {

    private static final String DISPATCH_CONFIG_KEY = "dispatch:cost:components";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public DispatchConfigSnapshot getCurrentConfig() {
        try {
            String payload = redisTemplate.opsForValue().get(DISPATCH_CONFIG_KEY);
            if (payload == null || payload.isBlank()) {
                return defaultSnapshot();
            }

            JsonNode node = objectMapper.readTree(payload);
            if (!node.isArray()) {
                return defaultSnapshot();
            }

            List<DispatchComponentConfig> components = new ArrayList<>();
            for (JsonNode item : node) {
                CostComponentKey key = CostComponentKey.valueOf(item.path("key").asText());
                components.add(DispatchComponentConfig.builder()
                        .key(key)
                        .enabled(item.path("enabled").asBoolean(true))
                        .weight(clampWeight(item.path("weight").asInt(10)))
                        .order(item.path("order").asInt(defaultOrder(key)))
                        .mandatory(isMandatory(key))
                        .build());
            }

            for (CostComponentKey key : EnumSet.allOf(CostComponentKey.class)) {
                boolean alreadyPresent = components.stream().anyMatch(c -> c.getKey() == key);
                if (!alreadyPresent) {
                    components.add(defaultConfig(key));
                }
            }

            components.sort(Comparator.comparingInt(DispatchComponentConfig::getOrder));
            return DispatchConfigSnapshot.builder().components(components).build();
        } catch (Exception ex) {
            log.warn("Failed to parse dispatch config from Redis. Falling back to defaults", ex);
            return defaultSnapshot();
        }
    }

    private DispatchConfigSnapshot defaultSnapshot() {
        List<DispatchComponentConfig> defaults = EnumSet.allOf(CostComponentKey.class).stream()
                .map(this::defaultConfig)
                .sorted(Comparator.comparingInt(DispatchComponentConfig::getOrder))
                .toList();
        return DispatchConfigSnapshot.builder().components(defaults).build();
    }

    private DispatchComponentConfig defaultConfig(CostComponentKey key) {
        return DispatchComponentConfig.builder()
                .key(key)
                .enabled(true)
                .weight(defaultWeight(key))
                .order(defaultOrder(key))
                .mandatory(isMandatory(key))
                .build();
    }

    private int defaultWeight(CostComponentKey key) {
        return switch (key) {
            case ETA_TOTAL_ESTIMATED, AVAILABILITY -> 10;
            case ROUTE_ALIGNMENT, TOUR_COMPATIBILITY -> 8;
            case PERFORMANCE_RATING -> 7;
            case RECENT_REFUSAL -> 6;
            case WORKLOAD_FAIRNESS -> 5;
            case MERCHANT_KNOWLEDGE -> 4;
            default -> 10;
        };
    }

    private int defaultOrder(CostComponentKey key) {
        return switch (key) {
            case ETA_TOTAL_ESTIMATED -> 1;
            case COURIER_TYPE -> 2;
            case AVAILABILITY -> 3;
            case ROUTE_ALIGNMENT -> 4;
            case TOUR_COMPATIBILITY -> 5;
            case PERFORMANCE_RATING -> 6;
            case GUARANTEED_DEADLINE -> 7;
            case RECENT_REFUSAL -> 8;
            case WORKLOAD_FAIRNESS -> 9;
            case MERCHANT_KNOWLEDGE -> 10;
            case VEHICLE_COMPATIBILITY -> 11;
        };
    }

    private boolean isMandatory(CostComponentKey key) {
        return key == CostComponentKey.GUARANTEED_DEADLINE || key == CostComponentKey.VEHICLE_COMPATIBILITY;
    }

    private int clampWeight(int weight) {
        return Math.max(0, Math.min(10, weight));
    }
}
