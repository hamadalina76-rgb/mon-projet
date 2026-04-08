package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MinItemsRuleTest {

    private MinItemsRule rule;

    @BeforeEach
    void setUp() {
        rule = new MinItemsRule();
    }

    @Test
    @DisplayName("MIN_ITEMS — insufficient items → KO")
    void shouldRejectTooFewItems() {
        PromotionRule promoRule = PromotionRule.builder()
                .ruleType(RuleType.MIN_ITEMS).targetValue("3").build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(Promotion.builder().id(1L).code("P").type(PromotionType.PERCENTAGE).isActive(true).build())
                .userId(1L).orderSubtotal(BigDecimal.TEN).itemCount(2).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> rule.evaluate(promoRule, ctx));
        assertEquals("MIN_ITEMS_NOT_MET", ex.getErrorCode());
    }

    @Test
    @DisplayName("MIN_ITEMS — sufficient items → OK")
    void shouldPassWithEnoughItems() {
        PromotionRule promoRule = PromotionRule.builder()
                .ruleType(RuleType.MIN_ITEMS).targetValue("3").build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(Promotion.builder().id(1L).code("P").type(PromotionType.PERCENTAGE).isActive(true).build())
                .userId(1L).orderSubtotal(BigDecimal.TEN).itemCount(5).build();

        assertDoesNotThrow(() -> rule.evaluate(promoRule, ctx));
    }
}
