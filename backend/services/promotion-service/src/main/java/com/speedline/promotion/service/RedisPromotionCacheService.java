package com.speedline.promotion.service;

import com.speedline.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Maintains a Redis SET of active promo codes for O(1) lookups.
 * Key: "promo:active-codes"
 * <p>
 * Lifecycle:
 * - Startup → warm cache from DB
 * - Create / Activate → addCode
 * - Deactivate / Expire / Delete → removeCode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPromotionCacheService {

    private static final String ACTIVE_CODES_KEY = "promo:active-codes";

    private final StringRedisTemplate redisTemplate;
    private final PromotionRepository promotionRepository;

    /**
     * Startup: load all active promo codes into Redis SET.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        try {
            redisTemplate.delete(ACTIVE_CODES_KEY);
            var codes = promotionRepository.findActivePromotions(LocalDateTime.now())
                    .stream().map(p -> p.getCode()).toArray(String[]::new);
            if (codes.length > 0) {
                redisTemplate.opsForSet().add(ACTIVE_CODES_KEY, codes);
            }
            log.info("[RedisCache] Warmed {} active promo codes", codes.length);
        } catch (Exception e) {
            log.warn("[RedisCache] Cache warm failed (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * O(1) check if a code is active.
     */
    public boolean isCodeActive(String code) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(ACTIVE_CODES_KEY, code.toUpperCase()));
    }

    /**
     * Add a code to the active set (on create / activate).
     */
    public void addCode(String code) {
        try {
            redisTemplate.opsForSet().add(ACTIVE_CODES_KEY, code.toUpperCase());
        } catch (Exception e) {
            log.warn("[RedisCache] addCode failed: {}", e.getMessage());
        }
    }

    /**
     * Remove a code from the active set (on deactivate / expire / delete).
     */
    public void removeCode(String code) {
        try {
            redisTemplate.opsForSet().remove(ACTIVE_CODES_KEY, code.toUpperCase());
        } catch (Exception e) {
            log.warn("[RedisCache] removeCode failed: {}", e.getMessage());
        }
    }
}
