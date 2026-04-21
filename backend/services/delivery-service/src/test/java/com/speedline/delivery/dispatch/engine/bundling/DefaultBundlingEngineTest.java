package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultBundlingEngineTest {

    private DefaultBundlingEngine engine;

    @BeforeEach
    void setUp() {
        DispatchProperties props = new DispatchProperties();
        props.getBundling().setEnabled(true);
        props.getBundling().setMaxBundleSize(4);
        props.getBundling().setDropoffRadiusMeters(800);
        props.getBundling().setMerchantRadiusMeters(1500);
        props.getBundling().setTimeWindowSeconds(8 * 60);
        props.getBundling().setAverageSpeedKmh(25.0);
        props.getBundling().setRespectUrgentFlag(true);

        engine = new DefaultBundlingEngine(props, new BundleFeasibilityChecker(), Mockito.mock(DispatchMetrics.class));
    }

    @Test
    void threeOrdersSameMerchantWithin500m_createsBundle() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 100L, 36.8000, 10.1800, 36.8100, 10.1900, 30, 0, false),
                order(2L, 100L, 36.8001, 10.1801, 36.8102, 10.1901, 30, 60, false),
                order(3L, 100L, 36.8000, 10.1800, 36.8103, 10.1902, 30, 120, false)
        ));

        assertEquals(1, result.getBundleToOrders().size());
        assertEquals(3, result.getBundleToOrders().values().iterator().next().size());
    }

    @Test
    void thirdOrderAt1200m_excluded() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 100L, 36.8000, 10.1800, 36.8100, 10.1900, 30, 0, false),
                order(2L, 100L, 36.8000, 10.1800, 36.8101, 10.1901, 30, 40, false),
                order(3L, 100L, 36.8000, 10.1800, 36.8210, 10.2030, 30, 60, false)
        ));

        assertEquals(1, result.getBundleToOrders().size());
        assertFalse(result.getOrderToBundleId().containsKey(3L));
    }

    @Test
    void fiveNearbyOrders_maxFourInBundle_fifthIndividual() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 100L, 36.8000, 10.1800, 36.8100, 10.1900, 30, 0, false),
                order(2L, 100L, 36.8000, 10.1800, 36.8101, 10.1901, 30, 40, false),
                order(3L, 100L, 36.8000, 10.1800, 36.8102, 10.1902, 30, 80, false),
                order(4L, 100L, 36.8000, 10.1800, 36.8103, 10.1903, 30, 120, false),
                order(5L, 100L, 36.8000, 10.1800, 36.8104, 10.1904, 30, 160, false)
        ));

        assertEquals(4, result.getBundleToOrders().values().iterator().next().size());
        assertFalse(result.getOrderToBundleId().containsKey(5L));
    }

    @Test
    void bundleBreachingGuarantee_notCreated() {
        BundlingResult result = engine.detectBundles(List.of(
                order(1L, 100L, 36.8000, 10.1800, 36.8500, 10.2300, 1, 0, false),
                order(2L, 100L, 36.8000, 10.1800, 36.8510, 10.2310, 1, 40, false)
        ));

        assertTrue(result.getBundleToOrders().isEmpty());
    }

    private static PendingOrder order(Long id,
                                      Long partnerId,
                                      double partnerLat,
                                      double partnerLon,
                                      double customerLat,
                                      double customerLon,
                                      int guaranteed,
                                      int offsetSeconds,
                                      boolean urgent) {
        return PendingOrder.builder()
                .id(id)
                .partnerId(partnerId)
                .customerId(10_000L + id)
                .partnerLat(partnerLat)
                .partnerLon(partnerLon)
                .customerLat(customerLat)
                .customerLon(customerLon)
                .zoneId(1L)
                .guaranteedDeliveryMinutes(guaranteed)
                .isUrgent(urgent)
                .isLargeOrder(false)
                .isScheduled(false)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z").plusSeconds(offsetSeconds))
                .build();
    }
}
