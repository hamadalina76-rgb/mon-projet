package com.speedline.delivery.dispatch.resilience;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.e2e.support.DispatchE2EBaseTest;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.service.DispatchCycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Disp106ResilienceTest extends DispatchE2EBaseTest {

    @Autowired
    private DispatchCycleService dispatchCycleService;

    @Test
    void redis_outage_then_recovery() {
        seedCourier(7401L, 1L, "INTERNAL", "IDLE");
        pushOrderCreated(90401L, 101L, 201L, 1L, false, Instant.now());

        REDIS.stop();
        assertThrows(Exception.class, () -> dispatchCycleService.runCycle(1L));

        REDIS.start();
        dispatchCycleService.runCycle(1L);
        await().untilAsserted(() -> org.junit.jupiter.api.Assertions.assertEquals(0, pendingOrders.countByZone(1L)));
    }

    @Test
    void pubsub_outage_producer_degrades_gracefully() {
        PUBSUB.stop();
        assertDoesNotThrow(() -> deliveryEventProducer.publishAssigned(DispatchAssignedEvent.builder()
                .orderId(90411L)
                .courierId(7402L)
                .zoneId(1L)
                .dispatchMode(DispatchMode.AUTO)
                .build()));
        PUBSUB.start();
    }

    @Test
    void solver_outage_fallback_still_assigns() {
        seedCourier(7403L, 1L, "INTERNAL", "IDLE");
        seedCourier(7404L, 1L, "INTERNAL", "IDLE");
        pushOrderCreated(90421L, 131L, 231L, 1L, true, Instant.now());
        pushOrderCreated(90422L, 131L, 232L, 1L, true, Instant.now());
        pushOrderCreated(90423L, 131L, 233L, 1L, true, Instant.now());

        SOLVER.stop();
        dispatchCycleService.runCycle(1L);

        await().untilAsserted(() ->
                org.junit.jupiter.api.Assertions.assertTrue(pendingOrders.countByZone(1L) < 3));
        SOLVER.start();
    }
}

