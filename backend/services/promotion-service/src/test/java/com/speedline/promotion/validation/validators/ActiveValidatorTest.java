package com.speedline.promotion.validation.validators;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ActiveValidatorTest {

    private ActiveValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ActiveValidator();
    }

    @Test
    @DisplayName("Cas 3 — Code désactivé → 'Promotion plus active'")
    void shouldThrowWhenInactive() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE)
                .isActive(false).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("PROMOTION_INACTIVE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Code actif → passe")
    void shouldPassWhenActive() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE)
                .isActive(true).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
