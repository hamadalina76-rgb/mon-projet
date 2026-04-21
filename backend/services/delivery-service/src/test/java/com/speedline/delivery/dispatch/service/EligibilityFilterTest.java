package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

class EligibilityFilterTest {

    @Mock
    private DispatchZoneConfig zoneConfig;
    @Mock
    private DispatchMetrics dispatchMetrics;

    private EligibilityFilter filter;
    private DispatchProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new DispatchProperties();
        properties.getEligibility().setOrderWaitingThresholdSeconds(180);
        properties.getEligibility().setInternalShortageThreshold(2);
        when(zoneConfig.getInternalShortageThreshold(anyLong())).thenReturn(2);
        filter = new EligibilityFilter(properties, zoneConfig, dispatchMetrics);
    }

    @Test
    void phase1_onlyInternalsWhenThresholdMet() {
        Instant now = Instant.parse("2026-04-21T10:00:00Z");
        List<AvailableCourier> out = filter.selectPool(
            List.of(order(1L, now)),
                List.of(internal(1L), internal(2L), internal(3L), external(10L), external(11L)),
                1L,
            Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(3, out.size());
        assertTrue(out.stream().allMatch(c -> c.getType() == CourierType.INTERNAL));
    }

    @Test
    void phase2_activatedWhenNoInternal() {
        Instant now = Instant.parse("2026-04-21T10:00:00Z");
        List<AvailableCourier> out = filter.selectPool(
            List.of(order(1L, now)),
                List.of(external(10L), external(11L), external(12L)),
                1L,
            Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(3, out.size());
        assertTrue(out.stream().allMatch(c -> c.getType() == CourierType.EXTERNAL));
    }

    @Test
    void phase2_activatedWhenOrderWaitingTooLong() {
        Instant now = Instant.parse("2026-04-21T10:00:00Z");
        List<AvailableCourier> out = filter.selectPool(
                List.of(order(1L, now.minusSeconds(400))),
                List.of(internal(1L), external(10L), external(11L)),
                1L,
                Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(3, out.size());
    }

    @Test
    void phase2_activatedWhenInternalShortage() {
        when(zoneConfig.getInternalShortageThreshold(1L)).thenReturn(3);
        Instant now = Instant.parse("2026-04-21T10:00:00Z");

        List<AvailableCourier> out = filter.selectPool(
            List.of(order(1L, now)),
                List.of(internal(1L), internal(2L), external(10L)),
                1L,
            Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(3, out.size());
    }

    @Test
    void shiftFiltering_excludesInternalsOutOfShift() {
        AvailableCourier c = internal(1L);
        c.setShiftStart(LocalTime.of(8, 0));
        c.setShiftEnd(LocalTime.of(9, 0));
        Instant now = Instant.parse("2026-04-21T10:00:00Z");

        List<AvailableCourier> out = filter.selectPool(
            List.of(order(1L, now)),
                List.of(c),
                1L,
            Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(0, out.size());
    }

    @Test
    void shiftFiltering_handlesOvernightShift() {
        AvailableCourier c = internal(1L);
        c.setShiftStart(LocalTime.of(22, 0));
        c.setShiftEnd(LocalTime.of(6, 0));
        Instant now = Instant.parse("2026-04-21T03:00:00Z");

        List<AvailableCourier> out = filter.selectPool(
            List.of(order(1L, now)),
                List.of(c),
                1L,
            Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(1, out.size());
    }

    private static AvailableCourier internal(Long id) {
        return AvailableCourier.builder().id(id).type(CourierType.INTERNAL).status(CourierStatus.IDLE).build();
    }

    private static AvailableCourier external(Long id) {
        return AvailableCourier.builder().id(id).type(CourierType.EXTERNAL).status(CourierStatus.IDLE).build();
    }

    private static PendingOrder order(Long id, Instant createdAt) {
        return PendingOrder.builder().id(id).createdAt(createdAt).build();
    }

}
