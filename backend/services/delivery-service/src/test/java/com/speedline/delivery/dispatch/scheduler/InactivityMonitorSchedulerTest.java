package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.service.CourierAvailabilityService;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InactivityMonitorSchedulerTest {

    @Mock
    private DispatchZoneConfig zoneConfig;
    @Mock
    private CourierAvailabilityService courierAvailabilityService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    private InactivityMonitorScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DispatchProperties properties = new DispatchProperties();
        properties.getInactivity().setThresholdMinutes(15);
        scheduler = new InactivityMonitorScheduler(zoneConfig, properties, courierAvailabilityService, redisTemplate, deliveryEventProducer);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(zoneConfig.getActiveZones()).thenReturn(List.of(zone(1L)));
    }

    @Test
    void alertsWhenInternalIdleBeyondThresholdDuringShift() {
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(internalIdle(10L)));
        when(valueOperations.get("courier:10:lastAssignedAt")).thenReturn("2000-01-01T00:00:00Z");
        when(redisTemplate.hasKey("courier:10:inactivity:alerted")).thenReturn(false);

        scheduler.checkInactiveInternalCouriers();

        verify(deliveryEventProducer).publishInactivityAlert(any());
    }

    @Test
    void noAlertWhenOutsideShift() {
        AvailableCourier c = internalIdle(10L);
        c.setShiftStart(LocalTime.of(8, 0));
        c.setShiftEnd(LocalTime.of(9, 0));
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(c));

        scheduler.checkInactiveInternalCouriers();

        verify(deliveryEventProducer, never()).publishInactivityAlert(any());
    }

    @Test
    void antiSpamAfterFirstAlert() {
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(internalIdle(10L)));
        when(valueOperations.get(anyString())).thenReturn("2026-04-21T09:00:00Z");
        when(redisTemplate.hasKey("courier:10:inactivity:alerted")).thenReturn(true);

        scheduler.checkInactiveInternalCouriers();

        verify(deliveryEventProducer, never()).publishInactivityAlert(any());
    }

    private static DispatchProperties.ZoneConfig zone(Long id) {
        DispatchProperties.ZoneConfig zone = new DispatchProperties.ZoneConfig();
        zone.setId(id);
        zone.setMode(DispatchMode.AUTO);
        return zone;
    }

    private static AvailableCourier internalIdle(Long id) {
        return AvailableCourier.builder()
                .id(id)
                .zoneId(1L)
                .type(CourierType.INTERNAL)
                .status(CourierStatus.IDLE)
                .build();
    }
}
