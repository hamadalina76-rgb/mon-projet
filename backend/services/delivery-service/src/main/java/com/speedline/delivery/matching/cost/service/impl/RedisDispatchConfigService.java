package com.speedline.delivery.matching.cost.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.service.DispatchConfigRuntimeWriter;
import com.speedline.delivery.matching.cost.service.DispatchConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDispatchConfigService implements DispatchConfigService, DispatchConfigRuntimeWriter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private volatile long localVersion = Long.MIN_VALUE;
    private volatile DispatchConfigSnapshot cachedSnapshot = null;

    @Override
    public DispatchConfigSnapshot getCurrentConfig() {
        try {
            String versionStr = redisTemplate.opsForValue().get(KEY_CONFIG_VERSION);
            long remote = versionStr == null || versionStr.isBlank() ? -1L : Long.parseLong(versionStr.trim());
            if (cachedSnapshot != null && remote == localVersion) {
                return cachedSnapshot;
            }
            DispatchConfigSnapshot parsed = parseSnapshotFromRedis();
            localVersion = remote;
            cachedSnapshot = parsed;
            return parsed;
        } catch (Exception ex) {
            log.warn("Failed to read dispatch config from Redis; using defaults", ex);
            return defaultSnapshot();
        }
    }

    @Override
    public void publishAtomic(long version, String componentsJson, String generalJson,
                              String internalExternalJson, String bundlingJson, String exclusivityJson) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().set(KEY_CONFIG_VERSION, Long.toString(version));
                operations.opsForValue().set(KEY_COST_COMPONENTS, componentsJson);
                operations.opsForValue().set(KEY_GENERAL, generalJson);
                operations.opsForValue().set(KEY_INTERNAL_EXTERNAL, internalExternalJson);
                operations.opsForValue().set(KEY_BUNDLING, bundlingJson);
                operations.opsForValue().set(KEY_EXCLUSIVITY, exclusivityJson);
                return operations.exec();
            }
        });
        localVersion = Long.MIN_VALUE;
        cachedSnapshot = null;
    }

    @Override
    public long readPublishedVersion() {
        String v = redisTemplate.opsForValue().get(KEY_CONFIG_VERSION);
        if (v == null || v.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private DispatchConfigSnapshot parseSnapshotFromRedis() {
        String payload = redisTemplate.opsForValue().get(KEY_COST_COMPONENTS);
        if (payload == null || payload.isBlank()) {
            return defaultSnapshot();
        }
        return parseComponentsArray(payload);
    }

    public DispatchConfigSnapshot parseComponentsArray(String payload) {
        try {
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
            log.warn("Failed to parse scoring payload; using defaults", ex);
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
