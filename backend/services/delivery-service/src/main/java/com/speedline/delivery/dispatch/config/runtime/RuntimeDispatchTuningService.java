package com.speedline.delivery.dispatch.config.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.matching.cost.service.DispatchConfigRuntimeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads merged runtime dispatch JSON from Redis (DISP-205) with YAML fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuntimeDispatchTuningService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DispatchProperties dispatchProperties;

    private volatile long cachedRedisVersion = Long.MIN_VALUE;
    private volatile DispatchProperties.Bundling cachedBundling;
    private volatile int cachedResponseTimeoutSeconds;
    private volatile int cachedLockTtlSeconds;

    public DispatchProperties.Bundling bundling() {
        refreshIfNeeded();
        return cachedBundling != null ? cachedBundling : dispatchProperties.getBundling();
    }

    public int responseTimeoutDeadlineSeconds() {
        refreshIfNeeded();
        return cachedResponseTimeoutSeconds > 0
                ? cachedResponseTimeoutSeconds
                : dispatchProperties.getResponseTimeout().getDeadlineSeconds();
    }

    public int lockTtlSeconds() {
        refreshIfNeeded();
        return cachedLockTtlSeconds > 0
                ? cachedLockTtlSeconds
                : dispatchProperties.getLock().getTtlSeconds();
    }

    private void refreshIfNeeded() {
        String v = redisTemplate.opsForValue().get(DispatchConfigRuntimeWriter.KEY_CONFIG_VERSION);
        long remote = parseLong(v, -1L);
        if (remote == cachedRedisVersion && cachedBundling != null) {
            return;
        }
        cachedBundling = readBundling(remote);
        readGeneral(remote);
        cachedRedisVersion = remote;
    }

    private void readGeneral(long remote) {
        String g = redisTemplate.opsForValue().get(DispatchConfigRuntimeWriter.KEY_GENERAL);
        if (g == null || g.isBlank()) {
            cachedResponseTimeoutSeconds = dispatchProperties.getResponseTimeout().getDeadlineSeconds();
            cachedLockTtlSeconds = dispatchProperties.getLock().getTtlSeconds();
            return;
        }
        try {
            var node = objectMapper.readTree(g);
            cachedResponseTimeoutSeconds = node.path("responseTimeoutSeconds").asInt(
                    dispatchProperties.getResponseTimeout().getDeadlineSeconds());
            cachedLockTtlSeconds = node.path("lockTtlSeconds").asInt(dispatchProperties.getLock().getTtlSeconds());
        } catch (Exception ex) {
            log.debug("General runtime JSON parse failed; using YAML: {}", ex.getMessage());
            cachedResponseTimeoutSeconds = dispatchProperties.getResponseTimeout().getDeadlineSeconds();
            cachedLockTtlSeconds = dispatchProperties.getLock().getTtlSeconds();
        }
    }

    private DispatchProperties.Bundling readBundling(long remote) {
        String b = redisTemplate.opsForValue().get(DispatchConfigRuntimeWriter.KEY_BUNDLING);
        if (b == null || b.isBlank()) {
            return dispatchProperties.getBundling();
        }
        try {
            DispatchProperties.Bundling parsed = objectMapper.readValue(b, DispatchProperties.Bundling.class);
            return parsed != null ? parsed : dispatchProperties.getBundling();
        } catch (Exception ex) {
            log.debug("Bundling runtime JSON parse failed; using YAML: {}", ex.getMessage());
            return dispatchProperties.getBundling();
        }
    }

    private static long parseLong(String v, long d) {
        if (v == null || v.isBlank()) {
            return d;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException ex) {
            return d;
        }
    }
}
