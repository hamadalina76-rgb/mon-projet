package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleFeasibilityCheckerTest {

    private final BundleFeasibilityChecker checker = new BundleFeasibilityChecker();

    @Test
    void boundaryOnSpeedAndGuarantee() {
        PendingOrder a = order(1L, 36.8, 10.1, 36.81, 10.11, 8);
        PendingOrder b = order(2L, 36.8, 10.1, 36.812, 10.112, 8);

        assertTrue(checker.isFeasible(List.of(a, b), 60.0).feasible());
        assertFalse(checker.isFeasible(List.of(a, b), 5.0).feasible());
    }

    private static PendingOrder order(Long id, double pl, double po, double cl, double co, int guaranteed) {
        return PendingOrder.builder()
                .id(id)
                .partnerId(100L)
                .customerId(1000L + id)
                .partnerLat(pl)
                .partnerLon(po)
                .customerLat(cl)
                .customerLon(co)
                .guaranteedDeliveryMinutes(guaranteed)
                .createdAt(Instant.now())
                .build();
    }
}
