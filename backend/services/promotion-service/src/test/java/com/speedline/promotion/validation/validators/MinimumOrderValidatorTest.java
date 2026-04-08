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

class MinimumOrderValidatorTest {

    private MinimumOrderValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MinimumOrderValidator();
    }

    @Test
    @DisplayName("Cas 8 — Subtotal 15 TND, minimum 20 TND → 'Commande minimum 20 TND requise'")
    void shouldThrowWhenSubtotalBelowMinimum() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .minimumOrder(new BigDecimal("20")).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(new BigDecimal("15")).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("MINIMUM_ORDER_NOT_MET", ex.getErrorCode());
    }

    @Test
    @DisplayName("Subtotal suffisant → passe")
    void shouldPassWhenSubtotalMeetsMinimum() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .minimumOrder(new BigDecimal("20")).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(new BigDecimal("25")).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
