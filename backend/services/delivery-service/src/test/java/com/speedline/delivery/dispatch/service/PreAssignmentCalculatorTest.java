package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PreAssignmentCalculatorTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private PreAssignmentCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DispatchProperties properties = new DispatchProperties();
        properties.getPreAssignment().setFinishWindowSeconds(180);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        calculator = new PreAssignmentCalculator(redisTemplate, properties);
    }

    @Test
    void includesCourierFinishingWithinWindow() {
        Instant now = Instant.parse("2026-04-21T10:00:00Z");
        when(valueOperations.get(anyString())).thenReturn(now.plusSeconds(120).toString());

        List<AvailableCourier> out = calculator.enrichPreAssignable(
                List.of(onDeliveryInternal(1L)),
                1L,
                Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(1, out.size());
        assertEquals(CourierStatus.PRE_ASSIGNABLE, out.get(0).getStatus());
    }

    @Test
    void excludesCourierFinishingBeyondWindow() {
        Instant now = Instant.parse("2026-04-21T10:00:00Z");
        when(valueOperations.get(anyString())).thenReturn(now.plusSeconds(300).toString());

        List<AvailableCourier> out = calculator.enrichPreAssignable(
                List.of(onDeliveryInternal(1L)),
                1L,
                Clock.fixed(now, ZoneOffset.UTC));

        assertEquals(0, out.size());
    }

    @Test
    void skipsWhenEtaFinishMissing() {
        when(valueOperations.get(anyString())).thenReturn(null);

        List<AvailableCourier> out = calculator.enrichPreAssignable(
                List.of(onDeliveryInternal(1L)),
                1L,
                Clock.systemUTC());

        assertEquals(0, out.size());
    }

    private static AvailableCourier onDeliveryInternal(Long id) {
        return AvailableCourier.builder()
                .id(id)
                .type(CourierType.INTERNAL)
                .status(CourierStatus.ON_DELIVERY)
                .build();
    }
}
