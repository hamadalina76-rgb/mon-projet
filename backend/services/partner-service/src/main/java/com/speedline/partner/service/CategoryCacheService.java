package com.speedline.partner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "categories:";
    private static final String ACTIVE_KEY   = "categories:active";

    /**
     * Invalider le cache des catégories actives
     */
    public void invalidateActiveCache() {
        try {
            redisTemplate.delete(ACTIVE_KEY);
            log.info("✅ Cache Redis invalidé: {}", ACTIVE_KEY);
        } catch (Exception e) {
            log.error("❌ Erreur invalidation cache Redis: {}", e.getMessage());
        }
    }

    /**
     * Invalider tous les caches liés aux catégories
     */
    public void invalidateAllCategoryCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("✅ {} clés Redis invalidées", keys.size());
            }
        } catch (Exception e) {
            log.error("❌ Erreur invalidation cache Redis: {}", e.getMessage());
        }
    }
}