package com.speedline.delivery.dispatch.e2e;

import com.speedline.delivery.dispatch.e2e.support.DispatchE2EBaseTest;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class Disp106FullFlowE2ETest extends DispatchE2EBaseTest {

    @Autowired
    private DispatchCycleService dispatchCycleService;

    @Test
    void orderCreated_to_cycle_to_dispatchAssigned() {
        seedCourier(7001L, 1L, "INTERNAL", "IDLE");
        pushOrderCreated(90001L, 101L, 201L, 1L, false, Instant.now());

        await().untilAsserted(() -> org.junit.jupiter.api.Assertions.assertEquals(1, pendingOrders.countByZone(1L)));

        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() -> org.junit.jupiter.api.Assertions.assertEquals(0, pendingOrders.countByZone(1L)));
        verify(deliveryEventProducer, atLeastOnce()).publishAssigned(any());
    }
}

