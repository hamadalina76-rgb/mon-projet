package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.domain.DeliveryStatus;
import com.speedline.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DispatchDashboardKpiServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private DispatchDashboardKpiService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new DispatchDashboardKpiService(deliveryRepository, redisTemplate);
    }

    @Test
    void computesExpectedKpisFromRedisAndPostgres() {
        when(valueOperations.get("dispatch:kpi:pending:total")).thenReturn("100");
        when(valueOperations.get("dispatch:kpi:first-cycle:assigned")).thenReturn("90");
        when(valueOperations.get("dispatch:kpi:active-couriers:last-hour")).thenReturn("10");
        when(valueOperations.get("dispatch:kpi:on-time:delivered")).thenReturn("92");
        when(valueOperations.get("dispatch:kpi:delivered:total")).thenReturn("100");
        when(valueOperations.get("dispatch:kpi:bundled:assigned")).thenReturn("40");
        when(valueOperations.get("dispatch:kpi:assigned:total")).thenReturn("100");
        when(valueOperations.get("dispatch:kpi:assignment-delay:avg-seconds")).thenReturn("75");
        when(deliveryRepository.countByStatusAndDeliveredAtAfter(eq(DeliveryStatus.DELIVERED), any(LocalDateTime.class))).thenReturn(30L);
        when(deliveryRepository.countByDeliveredAtIsNotNull()).thenReturn(200L);
        when(deliveryRepository.countByStatus(DeliveryStatus.FAILED)).thenReturn(2L);

        var response = service.getGlobalKpis();

        assertTrue(response.getFirstCycleDispatchRate() >= 85.0);
        assertTrue(response.getAverageAssignmentDelaySeconds() <= 90.0);
        assertTrue(response.getBundlingRate() >= 40.0);
        assertTrue(response.getFailureRate() <= 2.0);
    }
}
