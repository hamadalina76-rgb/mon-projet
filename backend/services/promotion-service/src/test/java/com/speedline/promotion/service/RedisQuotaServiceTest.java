package com.speedline.promotion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisQuotaServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private RedisQuotaService quotaService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        quotaService = new RedisQuotaService(redisTemplate);
    }

    @Test
    @DisplayName("remaining — lazy init from DB when key absent")
    void remainingInitFromDb() {
        when(valueOps.get("promo:quota:1")).thenReturn(null);
        long r = quotaService.remaining(1L, 100, 40);
        assertEquals(60, r);
        verify(valueOps).set("promo:quota:1", "60");
    }

    @Test
    @DisplayName("remaining — reads existing value from Redis")
    void remainingFromRedis() {
        when(valueOps.get("promo:quota:1")).thenReturn("25");
        assertEquals(25, quotaService.remaining(1L, 100, 40));
    }

    @Test
    @DisplayName("tryConsume — success when DECR >= 0")
    void tryConsumeSuccess() {
        when(valueOps.get("promo:quota:1")).thenReturn("5");
        when(valueOps.decrement("promo:quota:1")).thenReturn(4L);

        assertTrue(quotaService.tryConsume(1L, 10, 5));
        verify(valueOps, never()).increment("promo:quota:1");
    }

    @Test
    @DisplayName("tryConsume — fails and rolls back when DECR < 0")
    void tryConsumeFailRollback() {
        when(valueOps.get("promo:quota:1")).thenReturn("0");
        when(valueOps.decrement("promo:quota:1")).thenReturn(-1L);

        assertFalse(quotaService.tryConsume(1L, 10, 10));
        verify(valueOps).increment("promo:quota:1");
    }

    @Test
    @DisplayName("release — increments counter")
    void release() {
        when(redisTemplate.hasKey("promo:quota:1")).thenReturn(true);
        quotaService.release(1L);
        verify(valueOps).increment("promo:quota:1");
    }
}
