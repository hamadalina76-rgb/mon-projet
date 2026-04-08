package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.feign.OrderServiceClient;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstOrderRuleTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    private PromotionRule rule() {
        return PromotionRule.builder().ruleType(RuleType.FIRST_ORDER).build();
    }

    private ValidationContext ctx(Long userId) {
        return ValidationContext.builder()
                .promotion(Promotion.builder().id(1L).code("WELCOME").type(PromotionType.PERCENTAGE).isActive(true).build())
                .userId(userId)
                .orderSubtotal(BigDecimal.TEN)
                .build();
    }

    @Test
    @DisplayName("Cas 1 — FIRST_ORDER, user 0 commande → OK")
    void shouldPassForNewUser() {
        when(orderServiceClient.getOrderCount(1L)).thenReturn(0L);
        FirstOrderRule rule = new FirstOrderRule(orderServiceClient);
        assertDoesNotThrow(() -> rule.evaluate(rule(), ctx(1L)));
    }

    @Test
    @DisplayName("Cas 2 — FIRST_ORDER, user 5 commandes → KO")
    void shouldRejectReturningUser() {
        when(orderServiceClient.getOrderCount(1L)).thenReturn(5L);
        FirstOrderRule rule = new FirstOrderRule(orderServiceClient);
        PromotionException ex = assertThrows(PromotionException.class,
                () -> rule.evaluate(rule(), ctx(1L)));
        assertEquals("FIRST_ORDER_ONLY", ex.getErrorCode());
    }

    @Test
    @DisplayName("Cas 5 — Order Service down → fail-open (fallback returns 0)")
    void shouldPassWhenFeignFallbackReturnsZero() {
        // Simulates fallback behavior
        when(orderServiceClient.getOrderCount(1L)).thenReturn(0L);
        FirstOrderRule rule = new FirstOrderRule(orderServiceClient);
        assertDoesNotThrow(() -> rule.evaluate(rule(), ctx(1L)));
    }
}
