package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleAssignerTest {

    @Test
    void emptyInternals_returnsEmpty() {
        BundleAssigner assigner = new BundleAssigner(new DispatchProperties());
        assertTrue(assigner.pickCourier(List.of(order()), List.of()).isEmpty());
    }

    @Test
    void nearestPickSelection() {
        BundleAssigner assigner = new BundleAssigner(new DispatchProperties());
        AvailableCourier nearest = AvailableCourier.builder().id(1L).lat(36.8001).lon(10.1001).build();
        AvailableCourier far = AvailableCourier.builder().id(2L).lat(36.9000).lon(10.3000).build();

        Long picked = assigner.pickCourier(List.of(order()), List.of(far, nearest)).orElseThrow().getId();
        assertEquals(1L, picked);
    }

    private static PendingOrder order() {
        return PendingOrder.builder()
                .id(1L)
                .partnerId(10L)
                .partnerLat(36.8)
                .partnerLon(10.1)
                .customerLat(36.81)
                .customerLon(10.11)
                .createdAt(Instant.now())
                .build();
    }
}
