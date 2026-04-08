package com.speedline.promotion.feign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderServiceFallbackTest {

    @Test
    @DisplayName("Cas 5 — Fallback returns 0 (fail-open)")
    void fallbackReturnsZero() {
        OrderServiceFallback fallback = new OrderServiceFallback();
        assertEquals(0L, fallback.getOrderCount(42L));
    }
}
