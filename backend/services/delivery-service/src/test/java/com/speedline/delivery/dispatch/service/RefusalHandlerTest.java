package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefusalHandlerTest {

    @Mock
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Mock
    private RefusalCounterRepository refusalCounterRepository;
    @Mock
    private CourierResponseTimeoutTracker timeoutTracker;
    @Mock
    private DeliveryEventProducer deliveryEventProducer;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private DispatchMetrics dispatchMetrics;

    private RefusalHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DispatchProperties properties = new DispatchProperties();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        handler = new RefusalHandler(
                pendingOrderRedisRepository,
                refusalCounterRepository,
                timeoutTracker,
                deliveryEventProducer,
                properties,
                redisTemplate,
                dispatchMetrics);

        when(timeoutTracker.getTrackedOrder(anyLong())).thenReturn(Optional.of(PendingOrder.builder().id(1L).zoneId(1L).build()));
    }

    @Test
    void internalFirstRefusal_reQueuesAndIncrements() {
        when(valueOperations.get(anyString())).thenReturn("INTERNAL");
        when(refusalCounterRepository.incrementDailyRefusal(10L)).thenReturn(1L);

        handler.handleRefusal(1L, 10L, "OTHER");

        verify(pendingOrderRedisRepository).add(any(PendingOrder.class));
        verify(refusalCounterRepository).incrementDailyRefusal(10L);
        verify(refusalCounterRepository).blacklistOrderForCourier(1L, 10L);
    }

    @Test
    void internalThirdRefusal_triggersHrEvent() {
        when(valueOperations.get(anyString())).thenReturn("INTERNAL");
        when(refusalCounterRepository.incrementDailyRefusal(10L)).thenReturn(3L);

        handler.handleRefusal(1L, 10L, "OTHER");

        verify(deliveryEventProducer).publishRefusalEscalation(any());
    }

    @Test
    void externalFifthRefusal_triggersScoreDegradation() {
        when(valueOperations.get(anyString())).thenReturn("EXTERNAL");
        when(refusalCounterRepository.incrementDailyRefusal(20L)).thenReturn(5L);

        handler.handleRefusal(1L, 20L, "OTHER");

        verify(deliveryEventProducer).publishRefusalEscalation(any());
    }

    @Test
    void blacklistsCourierForOrderTtl() {
        when(valueOperations.get(anyString())).thenReturn("INTERNAL");
        when(refusalCounterRepository.incrementDailyRefusal(10L)).thenReturn(1L);

        handler.handleRefusal(1L, 10L, "OTHER");

        verify(refusalCounterRepository, times(1)).blacklistOrderForCourier(1L, 10L);
    }
}
