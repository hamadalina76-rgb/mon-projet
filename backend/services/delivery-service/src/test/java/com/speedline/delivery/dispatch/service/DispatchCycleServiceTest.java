package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DispatchCycleServiceTest {

    @Mock
    private DispatchZoneConfig zoneConfig;
    @Mock
    private PendingOrderRedisRepository pendingOrderRedisRepository;
    @Mock
    private CourierAvailabilityService courierAvailabilityService;
    @Mock
    private CostFunction costFunction;
    @Mock
    private DispatchSolver dispatchSolver;
    @Mock
    private BundlingEngine bundlingEngine;
    @Mock
    private PreAssignmentCalculator preAssignmentCalculator;
    @Mock
    private EligibilityFilter eligibilityFilter;
    @Mock
    private CourierResponseTimeoutTracker responseTimeoutTracker;
    @Mock
    private DeliveryEventProducer deliveryEventProducer;
    @Mock
    private DispatchMetrics dispatchMetrics;
    @Mock
    private DispatchRealtimePublisher dispatchRealtimePublisher;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private DispatchCycleService service;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        DispatchProperties properties = new DispatchProperties();
        properties.getLock().setTtlSeconds(30);

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
                dispatchRealtimePublisher,
                redisTemplate);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void happyPath_fiveOrdersThreeCouriers_threeAssignmentsPublished_twoUnmatched() {
        stubCycleDefaults();
        List<PendingOrder> orders = List.of(
                order(1L), order(2L), order(3L), order(4L), order(5L));
        List<AvailableCourier> couriers = List.of(courier(100L), courier(101L), courier(102L));
        List<Assignment> assignments = List.of(
                assignment(1L, 100L), assignment(2L, 101L), assignment(3L, 102L));

        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(orders);
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(couriers);
        when(dispatchSolver.solve(any())).thenReturn(assignments);
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(2L);

        service.runCycle(1L);

        verify(deliveryEventProducer, times(3)).publishAssigned(any(DispatchAssignedEvent.class));
        verify(pendingOrderRedisRepository, times(3)).remove(anyLong());
    }

    @Test
    void emptyCycleFast_under100ms() {
        stubCycleDefaults();
        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(List.of());
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(0L);

        assertTimeoutPreemptively(Duration.ofMillis(100), () -> service.runCycle(1L));

        verify(dispatchSolver, never()).solve(any());
        verify(deliveryEventProducer, never()).publishAssigned(any());
    }

    @Test
    void semiAuto_setsProposalTrue() {
        stubCycleDefaults();
        when(zoneConfig.getMode(1L)).thenReturn(DispatchMode.SEMI_AUTO);
        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(List.of(order(1L)));
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(courier(100L)));
        when(dispatchSolver.solve(any())).thenReturn(List.of(assignment(1L, 100L)));
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(0L);

        service.runCycle(1L);

        ArgumentCaptor<DispatchAssignedEvent> eventCaptor = ArgumentCaptor.forClass(DispatchAssignedEvent.class);
        verify(deliveryEventProducer).publishAssigned(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().isProposal());
        assertEquals(DispatchMode.SEMI_AUTO, eventCaptor.getValue().getDispatchMode());
    }

    @Test
    void manualZone_isSkipped() {
        when(zoneConfig.getMode(1L)).thenReturn(DispatchMode.MANUAL);

        service.runCycle(1L);

        verifyNoInteractions(pendingOrderRedisRepository);
        verifyNoInteractions(courierAvailabilityService);
        verifyNoInteractions(dispatchSolver);
    }

    @Test
    void capByZone_limitsOrdersProcessed() {
        stubCycleDefaults();
        when(zoneConfig.getMaxCapacity(1L)).thenReturn(2);
        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(List.of(order(1L), order(2L), order(3L), order(4L), order(5L)));
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(courier(100L), courier(101L), courier(102L)));
        when(dispatchSolver.solve(any())).thenReturn(List.of());
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(5L);

        service.runCycle(1L);

        verify(costFunction, times(2 * 3)).calculate(any(), any());
    }

    private static PendingOrder order(Long id) {
        return PendingOrder.builder()
                .id(id)
                .partnerId(10L)
                .customerId(20L)
                .zoneId(1L)
                .partnerLat(36.8)
                .partnerLon(10.1)
                .customerLat(36.81)
                .customerLon(10.11)
                .createdAt(Instant.now())
                .build();
    }

    private static AvailableCourier courier(Long id) {
        return AvailableCourier.builder()
                .id(id)
                .zoneId(1L)
                .type(CourierType.INTERNAL)
                .status(CourierStatus.IDLE)
                .lat(36.8)
                .lon(10.1)
                .rating(5.0)
                .currentLoad(0)
                .maxCapacity(1)
                .build();
    }

    private static Assignment assignment(Long orderId, Long courierId) {
        return Assignment.builder()
                .orderId(orderId)
                .courierId(courierId)
                .cost(2.0)
                .etaPickupMin(5)
                .etaDeliveryMin(15)
                .build();
    }

    private void stubCycleDefaults() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);
        when(zoneConfig.getMode(anyLong())).thenReturn(DispatchMode.AUTO);
        when(zoneConfig.getMaxCapacity(anyLong())).thenReturn(50);
        when(preAssignmentCalculator.enrichPreAssignable(anyList(), anyLong(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(eligibilityFilter.selectPool(anyList(), anyList(), anyLong(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(costFunction.calculate(any(), any())).thenReturn(CostResult.builder()
                .feasible(true)
                .totalCost(5.0)
                .etaPickupMin(5)
                .etaDeliveryMin(20)
                .build());
    }
}
