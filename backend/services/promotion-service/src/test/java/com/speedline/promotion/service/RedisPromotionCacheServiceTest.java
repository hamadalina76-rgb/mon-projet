package com.speedline.promotion.service;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisPromotionCacheServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SetOperations<String, String> setOps;
    @Mock private PromotionRepository promotionRepository;

    private RedisPromotionCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        cacheService = new RedisPromotionCacheService(redisTemplate, promotionRepository);
    }

    @Test
    @DisplayName("Cas 6 — warmCache charge les codes actifs dans Redis")
    void warmCacheLoadsActiveCodes() {
        Promotion p1 = Promotion.builder().code("CODE1").type(PromotionType.PERCENTAGE).build();
        Promotion p2 = Promotion.builder().code("CODE2").type(PromotionType.FIXED_AMOUNT).build();
        when(promotionRepository.findActivePromotions(any(LocalDateTime.class)))
                .thenReturn(List.of(p1, p2));

        cacheService.warmCache();

        verify(redisTemplate).delete("promo:active-codes");
        verify(setOps).add(eq("promo:active-codes"), eq("CODE1"), eq("CODE2"));
    }

    @Test
    @DisplayName("isCodeActive returns true when code in Redis SET")
    void isCodeActiveTrue() {
        when(setOps.isMember("promo:active-codes", "PROMO10")).thenReturn(true);
        assertTrue(cacheService.isCodeActive("promo10"));
    }

    @Test
    @DisplayName("addCode ajoute dans le SET Redis")
    void addCode() {
        cacheService.addCode("NEW_CODE");
        verify(setOps).add("promo:active-codes", "NEW_CODE");
    }

    @Test
    @DisplayName("removeCode retire du SET Redis")
    void removeCode() {
        cacheService.removeCode("OLD_CODE");
        verify(setOps).remove("promo:active-codes", "OLD_CODE");
    }
}
