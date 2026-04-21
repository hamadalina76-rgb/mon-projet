package com.speedline.delivery.dispatch.e2e;

import com.speedline.delivery.dispatch.e2e.support.DispatchE2EBaseTest;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class Disp106BundlingE2ETest extends DispatchE2EBaseTest {

    @Autowired
    private DispatchCycleService dispatchCycleService;

    @Test
    void three_close_orders_are_assigned_with_bundle_hint() {
        seedCourier(7101L, 1L, "INTERNAL", "IDLE");
        seedCourier(7102L, 1L, "INTERNAL", "IDLE");

        pushOrderCreated(90101L, 101L, 201L, 1L, true, Instant.now());
        pushOrderCreated(90102L, 101L, 202L, 1L, true, Instant.now());
        pushOrderCreated(90103L, 101L, 203L, 1L, true, Instant.now());

        await().untilAsserted(() -> org.junit.jupiter.api.Assertions.assertEquals(3, pendingOrders.countByZone(1L)));

        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() -> org.junit.jupiter.api.Assertions.assertEquals(0, pendingOrders.countByZone(1L)));
        verify(deliveryEventProducer, atLeastOnce()).publishAssigned(argThat(event ->
                event != null && event.getBundleId() != null));
    }
}

