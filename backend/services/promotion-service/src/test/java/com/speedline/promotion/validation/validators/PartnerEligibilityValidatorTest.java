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

class PartnerEligibilityValidatorTest {

    private PartnerEligibilityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PartnerEligibilityValidator();
    }

    @Test
    @DisplayName("Cas 9 — Partenaire non éligible → 'Non valable chez ce partenaire'")
    void shouldThrowWhenPartnerNotAllowed() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .applicablePartnerIds("[10, 20, 30]").build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .partnerId(99L).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("PARTNER_NOT_ELIGIBLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Partenaire dans la liste → passe")
    void shouldPassWhenPartnerAllowed() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .applicablePartnerIds("[10, 20, 30]").build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .partnerId(20L).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }

    @Test
    @DisplayName("Aucune restriction partenaire → passe")
    void shouldPassWhenNoPartnerRestriction() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .applicablePartnerIds(null).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .partnerId(99L).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
