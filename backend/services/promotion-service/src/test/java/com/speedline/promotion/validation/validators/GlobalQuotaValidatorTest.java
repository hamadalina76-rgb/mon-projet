package com.speedline.promotion.validation.validators;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.service.RedisQuotaService;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalQuotaValidatorTest {

    @Mock
    private RedisQuotaService redisQuotaService;

    private GlobalQuotaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GlobalQuotaValidator(redisQuotaService);
    }

    @Test
    @DisplayName("Cas 6 — Quota global atteint → 'Promotion épuisée'")
    void shouldThrowWhenQuotaExhausted() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .usageLimit(100).usageCount(100).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN).build();

        when(redisQuotaService.remaining(1L, 100, 100)).thenReturn(0L);

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("PROMOTION_QUOTA_EXCEEDED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Quota restant → passe")
    void shouldPassWhenQuotaAvailable() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .usageLimit(100).usageCount(50).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN).build();

        when(redisQuotaService.remaining(1L, 100, 50)).thenReturn(50L);

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
