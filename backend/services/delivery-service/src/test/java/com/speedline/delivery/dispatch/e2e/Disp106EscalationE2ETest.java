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

class Disp106EscalationE2ETest extends DispatchE2EBaseTest {

    @Autowired
    private DispatchCycleService dispatchCycleService;

    @Test
    void no_internal_then_external_pool_is_eligible_after_waiting_threshold() {
        // No internal courier, only external one in zone.
        seedCourier(7301L, 1L, "EXTERNAL", "IDLE");

        // createdAt in the past to force phase-2 eligibility immediately in compressed test config.
        pushOrderCreated(90301L, 121L, 221L, 1L, false, Instant.now().minusSeconds(20));

        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() -> org.junit.jupiter.api.Assertions.assertEquals(0, pendingOrders.countByZone(1L)));
        verify(deliveryEventProducer, atLeastOnce()).publishAssigned(any());
    }
}

