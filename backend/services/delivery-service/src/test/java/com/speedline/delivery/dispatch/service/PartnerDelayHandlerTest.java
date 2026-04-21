package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerDelayHandlerTest {

    @Mock
    private PartnerDelayTrackerRepository delayTrackerRepository;
    @Mock
    private PartnerSlaCounterRepository partnerSlaCounterRepository;
    @Mock
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Mock
    private PendingOrderEnricher pendingOrderEnricher;
    @Mock
    private CourierResponseTimeoutTracker timeoutTracker;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private PartnerDelayHandler partnerDelayHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        DispatchProperties properties = new DispatchProperties();
        DispatchProperties.PartnerDelay partnerDelay = new DispatchProperties.PartnerDelay();
        partnerDelay.setThresholdMinutes(5);
        properties.setPartnerDelay(partnerDelay);
        partnerDelayHandler = new PartnerDelayHandler(
                delayTrackerRepository,
                partnerSlaCounterRepository,
                pendingOrderRedisRepository,
                pendingOrderEnricher,
                timeoutTracker,
                redisTemplate,
                properties
        );
    }

    @Test
    void delayAboveThreshold_releasesCourier_requeuesAndIncrementsSla() {
        Map<String, Object> event = Map.of(
                "orderId", 31L,
                "partnerId", 51L,
                "courierId", 71L,
                "delayStartedAt", Instant.now().minusSeconds(6 * 60L).toString()
        );
        when(delayTrackerRepository.findAll()).thenReturn(List.of(event));
        when(timeoutTracker.getTrackedOrder(31L)).thenReturn(Optional.of(PendingOrder.builder().id(31L).zoneId(2L).build()));

        partnerDelayHandler.processActiveDelays();

        verify(valueOperations).set("courier:71:status", "IDLE");
        verify(valueOperations).set("courier:71:isOnline", "true");
        verify(pendingOrderRedisRepository).add(any(PendingOrder.class));
        verify(partnerSlaCounterRepository).incrementMonthlyDelay(51L);
        verify(delayTrackerRepository).remove(31L);
    }

    @Test
    void delayBelowThreshold_doesNothing() {
        Map<String, Object> event = Map.of(
                "orderId", 32L,
                "partnerId", 52L,
                "courierId", 72L,
                "delayStartedAt", Instant.now().minusSeconds(2 * 60L).toString()
        );
        when(delayTrackerRepository.findAll()).thenReturn(List.of(event));

        partnerDelayHandler.processActiveDelays();

        verify(partnerSlaCounterRepository, never()).incrementMonthlyDelay(any());
        verify(pendingOrderRedisRepository, never()).add(any());
        verify(delayTrackerRepository, never()).remove(any());
    }
}
