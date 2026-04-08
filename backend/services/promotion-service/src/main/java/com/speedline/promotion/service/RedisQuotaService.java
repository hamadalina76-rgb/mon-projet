package com.speedline.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages per-promotion global quota counters in Redis.
 * <p>
 * Key format: {@code promo:quota:<promotionId>}
 * <p>
 * The counter stores the <b>remaining</b> usages.
 * On {@link #tryConsume} the key is atomically decremented (DECR);
 * if the value drops below 0 the decrement is rolled back (INCR)
 * and the call returns {@code false} → quota exhausted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisQuotaService {

    private static final String KEY_PREFIX = "promo:quota:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Returns the remaining quota for read-only validation.
     * If the key does not exist in Redis, it is lazily initialised from
     * the database values.
     */
    public long remaining(Long promotionId, int usageLimit, int usageCount) {
        String key = key(promotionId);
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            // Bootstrap from DB
            long remaining = (long) usageLimit - usageCount;
            redisTemplate.opsForValue().set(key, String.valueOf(remaining));
            return remaining;
        }
        return Long.parseLong(val);
    }

    /**
     * Atomically consume one unit of quota.
     *
     * @return {@code true} if the unit was successfully consumed,
     *         {@code false} if the quota is exhausted.
     */
    public boolean tryConsume(Long promotionId, int usageLimit, int usageCount) {
        String key = key(promotionId);
        // Ensure key exists
        remaining(promotionId, usageLimit, usageCount);

        Long after = redisTemplate.opsForValue().decrement(key);
        if (after != null && after >= 0) {
            return true;
        }
        // Roll back — quota was already 0
        redisTemplate.opsForValue().increment(key);
        return false;
    }

    /**
     * Give one unit back (used on revoke).
     */
    public void release(Long promotionId) {
        String key = key(promotionId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key);
        }
    }

    private String key(Long promotionId) {
        return KEY_PREFIX + promotionId;
    }
}
