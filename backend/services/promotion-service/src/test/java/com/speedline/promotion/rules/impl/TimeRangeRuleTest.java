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
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeRangeRuleTest {

    private TimeRangeRule rule;

    @BeforeEach
    void setUp() {
        rule = new TimeRangeRule();
    }

    private ValidationContext ctx() {
        return ValidationContext.builder()
                .promotion(Promotion.builder().id(1L).code("LUNCH").type(PromotionType.PERCENTAGE).isActive(true).build())
                .userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .build();
    }

    @Test
    @DisplayName("Cas 4 — TIME_RANGE when current time is outside → KO")
    void shouldRejectOutsideTimeRange() {
        // Build a range that definitely doesn't include now
        LocalTime now = LocalTime.now();
        String start = now.plusHours(2).withSecond(0).withNano(0).toString();
        String end = now.plusHours(4).withSecond(0).withNano(0).toString();

        PromotionRule promoRule = PromotionRule.builder()
                .ruleType(RuleType.TIME_RANGE).targetValue(start + "-" + end).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> rule.evaluate(promoRule, ctx()));
        assertEquals("OUTSIDE_TIME_RANGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("TIME_RANGE when current time is inside → OK")
    void shouldPassInsideTimeRange() {
        // Build a range that includes now
        LocalTime now = LocalTime.now();
        String start = now.minusHours(1).withSecond(0).withNano(0).toString();
        String end = now.plusHours(1).withSecond(0).withNano(0).toString();

        PromotionRule promoRule = PromotionRule.builder()
                .ruleType(RuleType.TIME_RANGE).targetValue(start + "-" + end).build();

        assertDoesNotThrow(() -> rule.evaluate(promoRule, ctx()));
    }
}
