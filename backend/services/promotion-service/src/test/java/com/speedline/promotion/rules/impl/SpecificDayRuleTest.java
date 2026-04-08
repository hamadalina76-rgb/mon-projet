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
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SpecificDayRuleTest {

    private SpecificDayRule rule;

    @BeforeEach
    void setUp() {
        rule = new SpecificDayRule();
    }

    private ValidationContext ctx() {
        return ValidationContext.builder()
                .promotion(Promotion.builder().id(1L).code("MONDAY10").type(PromotionType.PERCENTAGE).isActive(true).build())
                .userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .build();
    }

    @Test
    @DisplayName("Cas 3 — SPECIFIC_DAY=today → OK")
    void shouldPassOnCorrectDay() {
        String today = LocalDate.now().getDayOfWeek().name();
        PromotionRule promoRule = PromotionRule.builder()
                .ruleType(RuleType.SPECIFIC_DAY).targetValue(today).build();

        assertDoesNotThrow(() -> rule.evaluate(promoRule, ctx()));
    }

    @Test
    @DisplayName("SPECIFIC_DAY=wrong day → KO")
    void shouldRejectWrongDay() {
        // Pick a day that's NOT today
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        DayOfWeek other = today == DayOfWeek.MONDAY ? DayOfWeek.TUESDAY : DayOfWeek.MONDAY;

        PromotionRule promoRule = PromotionRule.builder()
                .ruleType(RuleType.SPECIFIC_DAY).targetValue(other.name()).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> rule.evaluate(promoRule, ctx()));
        assertEquals("WRONG_DAY", ex.getErrorCode());
    }
}
