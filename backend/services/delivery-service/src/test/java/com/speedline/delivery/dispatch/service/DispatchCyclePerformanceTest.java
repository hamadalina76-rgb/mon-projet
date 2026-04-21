package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.mock.MockBundlingEngine;
import com.speedline.delivery.dispatch.contract.mock.MockCostFunction;
import com.speedline.delivery.dispatch.contract.mock.MockDispatchSolver;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchCyclePerformanceTest {

    @Mock
    private DispatchZoneConfig zoneConfig;
    @Mock
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Mock
    private CourierAvailabilityService courierAvailabilityService;
    @Mock
    private DeliveryEventProducer deliveryEventProducer;
    @Mock
    private DispatchMetrics dispatchMetrics;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PreAssignmentCalculator preAssignmentCalculator;
    @Mock
    private EligibilityFilter eligibilityFilter;
    @Mock
    private CourierResponseTimeoutTracker responseTimeoutTracker;

    private DispatchCycleService service;

    @BeforeEach
    void setUp() {
        DispatchProperties properties = new DispatchProperties();
        properties.getLock().setTtlSeconds(30);

        CostFunction costFunction = new MockCostFunction();
        DispatchSolver dispatchSolver = new MockDispatchSolver();
        BundlingEngine bundlingEngine = new MockBundlingEngine();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);
        when(zoneConfig.getMode(1L)).thenReturn(DispatchMode.AUTO);
        when(zoneConfig.getMaxCapacity(1L)).thenReturn(200);

        service = new DispatchCycleService(
                zoneConfig,
                properties,
                pendingOrderRedisRepository,
                courierAvailabilityService,
                costFunction,
                dispatchSolver,
                bundlingEngine,
                preAssignmentCalculator,
                eligibilityFilter,
                responseTimeoutTracker,
                deliveryEventProducer,
                dispatchMetrics,
                redisTemplate);

            when(preAssignmentCalculator.enrichPreAssignable(anyList(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
            when(eligibilityFilter.selectPool(anyList(), anyList(), any(), any())).thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void hundredOrdersFiftyCouriers_cycleUnderThreeSeconds() {
        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(buildOrders(100));
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(buildCouriers(50));
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(0L);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> service.runCycle(1L));
    }

    private static List<PendingOrder> buildOrders(int n) {
        List<PendingOrder> out = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            out.add(PendingOrder.builder()
                    .id((long) i)
                    .partnerId(10L)
                    .customerId(20L)
                    .zoneId(1L)
                    .partnerLat(36.8)
                    .partnerLon(10.1)
                    .customerLat(36.81)
                    .customerLon(10.11)
                    .guaranteedDeliveryMinutes(30)
                    .createdAt(Instant.now())
                    .build());
        }
        return out;
    }

    private static List<AvailableCourier> buildCouriers(int n) {
        List<AvailableCourier> out = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            out.add(AvailableCourier.builder()
                    .id((long) i)
                    .zoneId(1L)
                    .type(CourierType.INTERNAL)
                    .status(CourierStatus.IDLE)
                    .lat(36.8)
                    .lon(10.1)
                    .rating(5.0)
                    .currentLoad(0)
                    .maxCapacity(1)
                    .build());
        }
        return out;
    }
}
