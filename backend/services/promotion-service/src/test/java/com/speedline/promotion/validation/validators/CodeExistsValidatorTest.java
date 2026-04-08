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

class CodeExistsValidatorTest {

    private CodeExistsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CodeExistsValidator();
    }

    @Test
    @DisplayName("Cas 2 — Code inexistant → 'Code introuvable'")
    void shouldThrowWhenPromotionIsNull() {
        ValidationContext ctx = ValidationContext.builder()
                .promotion(null)
                .userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("PROMOTION_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Code existant → passe au maillon suivant")
    void shouldPassWhenPromotionExists() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE)
                .isActive(true).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
