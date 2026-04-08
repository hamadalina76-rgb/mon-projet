package com.speedline.promotion.service;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscountCalculatorTest {

    private DiscountCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DiscountCalculator();
    }

    @Test
    @DisplayName("Cas 10 — 25% sur 40 TND, max 8 TND → discount = 8 (plafonné)")
    void percentageCappedByMaximumDiscount() {
        Promotion p = Promotion.builder()
                .type(PromotionType.PERCENTAGE)
                .value(new BigDecimal("25"))
                .maximumDiscount(new BigDecimal("8"))
                .build();

        BigDecimal discount = calculator.compute(p, new BigDecimal("40"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("8.00"), discount);
    }

    @Test
    @DisplayName("Cas 11 — 25% sur 20 TND, max 8 TND → discount = 5 (pas plafonné)")
    void percentageNotCapped() {
        Promotion p = Promotion.builder()
                .type(PromotionType.PERCENTAGE)
                .value(new BigDecimal("25"))
                .maximumDiscount(new BigDecimal("8"))
                .build();

        BigDecimal discount = calculator.compute(p, new BigDecimal("20"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("5.00"), discount);
    }

    @Test
    @DisplayName("Cas 12 — FIXED 15 TND sur commande 10 TND → discount = 10 (plafonné au subtotal)")
    void fixedAmountCappedBySubtotal() {
        Promotion p = Promotion.builder()
                .type(PromotionType.FIXED_AMOUNT)
                .value(new BigDecimal("15"))
                .build();

        BigDecimal discount = calculator.compute(p, new BigDecimal("10"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("10.00"), discount);
    }

    @Test
    @DisplayName("Cas 13 — FREE_DELIVERY, frais 4.50 → discount = 4.50, delivery = 0")
    void freeDelivery() {
        Promotion p = Promotion.builder()
                .type(PromotionType.FREE_DELIVERY)
                .value(BigDecimal.ZERO)
                .build();

        BigDecimal discount = calculator.compute(p, new BigDecimal("30"), new BigDecimal("4.50"));
        assertEquals(new BigDecimal("4.50"), discount);
    }

    @Test
    @DisplayName("FREE_DELIVERY sans frais → discount = 0")
    void freeDeliveryNoFee() {
        Promotion p = Promotion.builder()
                .type(PromotionType.FREE_DELIVERY)
                .value(BigDecimal.ZERO)
                .build();

        BigDecimal discount = calculator.compute(p, new BigDecimal("30"), null);
        assertEquals(new BigDecimal("0.00"), discount);
    }
}
