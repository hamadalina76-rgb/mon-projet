package com.speedline.delivery.dispatch.impl.bundling;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.runtime.RuntimeDispatchTuningService;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleBasedBundlingEngineTest {

    @Mock
    private SolverServiceClient solverServiceClient;
    @Mock
    private RuntimeDispatchTuningService runtimeDispatchTuningService;

    private DispatchProperties properties;
    private RuleBasedBundlingEngine engine;

    @BeforeEach
    void setUp() {
        properties = new DispatchProperties();
        properties.getBundling().setMaxBundleSize(4);
        properties.getBundling().setDropoffRadiusMeters(800);
        properties.getBundling().setMerchantRadiusMeters(1500);
        properties.getBundling().setTimeWindowSeconds(8 * 60);
        properties.getBundling().setAverageSpeedKmh(22.0);
        properties.getSolver().getOrtools().setEnabled(false);

        org.mockito.Mockito.when(runtimeDispatchTuningService.bundling()).thenReturn(properties.getBundling());

        engine = new RuleBasedBundlingEngine(properties, runtimeDispatchTuningService, solverServiceClient);
    }

    @Test
    void threeNearbyOrdersSamePartner_shouldCreateBundle() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 36.8000, 10.1800, 36.8100, 10.1900, 20, 0),
                order(2L, 36.8001, 10.1801, 36.8103, 10.1902, 20, 120),
                order(3L, 36.8000, 10.1800, 36.8104, 10.1904, 20, 240)
        ));

        assertEquals(1, result.getBundles().size());
        assertEquals(2, result.getBundles().get(0).size());
        assertNotNull(result.getOrderToBundleId().get(2L));
        assertEquals(result.getOrderToBundleId().get(2L), result.getOrderToBundleId().get(3L));
        assertFalse(result.getOrderToBundleId().containsKey(1L));
        assertTrue(result.getOrderEtaMinutes().containsKey(2L));
        assertTrue(result.getOrderEtaMinutes().containsKey(3L));
    }

    @Test
    void distantThirdOrder_shouldBeExcludedFromBundle() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 36.8000, 10.1800, 36.8100, 10.1900, 20, 0),
                order(2L, 36.8000, 10.1800, 36.8102, 10.1902, 20, 60),
                order(3L, 36.8000, 10.1800, 36.8200, 10.2000, 20, 120)
        ));

        assertTrue(result.getBundles().isEmpty());
        assertTrue(result.getOrderToBundleId().isEmpty());
    }

    @Test
    void fiveNearbyOrders_shouldLimitBundleToFour() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 36.8000, 10.1800, 36.8100, 10.1900, 30, 0),
                order(2L, 36.8000, 10.1800, 36.8101, 10.1901, 30, 60),
                order(3L, 36.8000, 10.1800, 36.8102, 10.1902, 30, 120),
                order(4L, 36.8000, 10.1800, 36.8103, 10.1903, 30, 180),
                order(5L, 36.8000, 10.1800, 36.8104, 10.1904, 30, 240)
        ));

        assertFalse(result.getBundles().isEmpty());
        assertTrue(result.getOrderToBundleId().containsKey(5L));
    }

    @Test
    void slaViolation_shouldRejectBundle() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 36.8000, 10.1800, 36.8160, 10.1960, 1, 0),
                order(2L, 36.8000, 10.1800, 36.8162, 10.1962, 1, 60)
        ));

        assertTrue(result.getBundles().isEmpty());
        assertTrue(result.getOrderToBundleId().isEmpty());
    }

    @Test
    void orToolsUnavailable_shouldFallbackToNearestNeighbor() {
        properties.getSolver().getOrtools().setEnabled(true);
        when(solverServiceClient.optimizeRoute(any())).thenThrow(new RuntimeException("OR-Tools down"));

        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 36.8000, 10.1800, 36.8100, 10.1900, 20, 0),
                order(2L, 36.8000, 10.1800, 36.8102, 10.1902, 20, 60),
                order(3L, 36.8000, 10.1800, 36.8103, 10.1903, 20, 120)
        ));

        verify(solverServiceClient).optimizeRoute(any());
        assertEquals(1, result.getBundles().size());
        assertEquals(2, result.getBundles().get(0).size());
    }

    private static PendingOrder order(Long id,
                                      double partnerLat,
                                      double partnerLon,
                                      double customerLat,
                                      double customerLon,
                                      int guaranteedMinutes,
                                      int createdOffsetSeconds) {
        return PendingOrder.builder()
                .id(id)
                .partnerId(100L)
                .customerId(1000L + id)
                .partnerLat(partnerLat)
                .partnerLon(partnerLon)
                .customerLat(customerLat)
                .customerLon(customerLon)
                .zoneId(1L)
                .guaranteedDeliveryMinutes(guaranteedMinutes)
                .isUrgent(false)
                .isLargeOrder(false)
                .isScheduled(false)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z").plusSeconds(createdOffsetSeconds))
                .build();
    }
}
