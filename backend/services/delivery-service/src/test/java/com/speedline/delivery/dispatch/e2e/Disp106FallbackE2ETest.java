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

class Disp106FallbackE2ETest extends DispatchE2EBaseTest {

    @Autowired
    private DispatchCycleService dispatchCycleService;

    @Test
    void when_solver_is_down_greedy_fallback_keeps_dispatch_running() {
        seedCourier(7201L, 1L, "INTERNAL", "IDLE");
        seedCourier(7202L, 1L, "INTERNAL", "IDLE");

        pushOrderCreated(90201L, 111L, 211L, 1L, true, Instant.now());
        pushOrderCreated(90202L, 111L, 212L, 1L, true, Instant.now());
        pushOrderCreated(90203L, 111L, 213L, 1L, true, Instant.now());

        // Kill fake solver -> selector should fallback to greedy branch.
        SOLVER.stop();

        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() ->
                org.junit.jupiter.api.Assertions.assertTrue(pendingOrders.countByZone(1L) < 3));
        verify(deliveryEventProducer, atLeastOnce()).publishAssigned(any());

        if (!SOLVER.isRunning()) {
            SOLVER.start();
        }
    }
}

