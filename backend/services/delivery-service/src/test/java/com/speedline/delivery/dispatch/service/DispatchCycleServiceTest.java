package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.config.runtime.RuntimeDispatchTuningService;
import com.speedline.delivery.dispatch.config.service.DispatchCycleCaptureRecorder;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.engine.bundling.BundleDispatchOrchestrator;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private BundleDispatchOrchestrator bundleDispatchOrchestrator;
    @Mock
    private PreAssignmentCalculator preAssignmentCalculator;
    @Mock
    private EligibilityFilter eligibilityFilter;
    @Mock
    private UrgentOrderPrePassService urgentOrderPrePassService;
    @Mock
    private UrgentBonusService urgentBonusService;
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
    @Mock
    private RuntimeDispatchTuningService runtimeDispatchTuningService;
    @Mock
    private DispatchCycleCaptureRecorder cycleCaptureRecorder;
    @Mock
    private DispatchProposalService dispatchProposalService;
    @Mock
    private DispatchDeliveryRecordService dispatchDeliveryRecordService;
    @Mock
    private OrderServiceClient orderServiceClient;
    @Mock
    private PendingOrderEnricher pendingOrderEnricher;

    private DispatchCycleService service;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        DispatchProperties properties = new DispatchProperties();
        properties.getLock().setTtlSeconds(30);
        org.mockito.Mockito.when(runtimeDispatchTuningService.responseTimeoutDeadlineSeconds())
                .thenReturn(properties.getResponseTimeout().getDeadlineSeconds());
        org.mockito.Mockito.when(runtimeDispatchTuningService.lockTtlSeconds())
                .thenReturn(properties.getLock().getTtlSeconds());

        service = new DispatchCycleService(
                zoneConfig,
                properties,
                pendingOrderRedisRepository,
                courierAvailabilityService,
                costFunction,
                dispatchSolver,
                bundlingEngine,
                bundleDispatchOrchestrator,
                preAssignmentCalculator,
                eligibilityFilter,
                urgentOrderPrePassService,
                urgentBonusService,
                responseTimeoutTracker,
                deliveryEventProducer,
                dispatchMetrics,
                dispatchRealtimePublisher,
                redisTemplate,
                runtimeDispatchTuningService,
                cycleCaptureRecorder,
                dispatchProposalService,
                dispatchDeliveryRecordService,
                orderServiceClient,
                pendingOrderEnricher);
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
    void semiAuto_enqueuesProposalWithoutPublishingDispatchEvent() {
        stubCycleDefaults();
        when(zoneConfig.getMode(1L)).thenReturn(DispatchMode.SEMI_AUTO);
        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(List.of(order(1L)));
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(courier(100L)));
        when(dispatchSolver.solve(any())).thenReturn(List.of(assignment(1L, 100L)));
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(0L);

        service.runCycle(1L);

        verify(deliveryEventProducer, never()).publishAssigned(any(DispatchAssignedEvent.class));
        verify(dispatchProposalService).enqueueProposal(eq(1L), any(Assignment.class), anyMap());
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

    @Test
    void bundledOrders_externalCourierIsRefusedAndExpandedToMembers() {
        stubCycleDefaults();

        PendingOrder order1 = order(1L);
        PendingOrder order2 = order(2L);
        List<PendingOrder> orders = List.of(order1, order2);
        AvailableCourier internal = courier(100L);
        AvailableCourier external = courier(200L);
        external.setType(CourierType.EXTERNAL);

        BundlingResult bundlingResult = BundlingResult.builder()
                .bundles(List.of(List.of(1L, 2L)))
                .orderToBundleId(java.util.Map.of(1L, 1L, 2L, 1L))
                .bundleToOrders(java.util.Map.of(1L, List.of(1L, 2L)))
                .orderEtaMinutes(java.util.Map.of(1L, 8, 2L, 12))
                .build();

        when(pendingOrderRedisRepository.findByZone(1L)).thenReturn(orders);
        when(courierAvailabilityService.findOnlineByZone(1L)).thenReturn(List.of(internal, external));
        when(bundlingEngine.detectBundles(anyList())).thenReturn(bundlingResult);
        when(pendingOrderRedisRepository.countByZone(1L)).thenReturn(0L);

        when(bundleDispatchOrchestrator.dispatchBundles(any(), any(), anyList(), anyList(), anyLong()))
            .thenReturn(com.speedline.delivery.dispatch.engine.bundling.BundleAssignmentBatch.builder()
                .assignments(List.of(
                    Assignment.builder().orderId(1L).courierId(100L).bundleId(1L).etaDeliveryMin(8).build(),
                    Assignment.builder().orderId(2L).courierId(100L).bundleId(1L).etaDeliveryMin(12).build()))
                .remainingOrders(List.of())
                .remainingCouriers(List.of(external))
                .build());

        service.runCycle(1L);

        ArgumentCaptor<DispatchAssignedEvent> captor = ArgumentCaptor.forClass(DispatchAssignedEvent.class);
        verify(deliveryEventProducer, times(2)).publishAssigned(captor.capture());
        List<DispatchAssignedEvent> events = captor.getAllValues();
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.getCourierId().equals(100L)));
        assertTrue(events.stream().allMatch(e -> e.getBundleId().equals(1L)));
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
        when(orderServiceClient.assignCourierToOrder(anyLong(), anyMap())).thenReturn(Map.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);
        when(dispatchProposalService.hasPendingProposal(anyLong())).thenReturn(false);
        when(zoneConfig.getMode(anyLong())).thenReturn(DispatchMode.AUTO);
        when(zoneConfig.getMaxCapacity(anyLong())).thenReturn(50);
        when(preAssignmentCalculator.enrichPreAssignable(anyList(), anyLong(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(eligibilityFilter.selectPoolDetailed(anyList(), anyList(), anyLong(), any())).thenAnswer(inv -> {
            List<AvailableCourier> pool = inv.getArgument(1);
            List<AvailableCourier> internals = pool.stream().filter(c -> c.getType() == CourierType.INTERNAL).toList();
            return EligibilityPool.builder().pool(pool).internalOnly(internals).fallbackApplied(false).build();
        });
        when(urgentOrderPrePassService.runPrePass(anyList(), anyList(), anyList(), anyLong()))
                .thenReturn(new UrgentOrderPrePassService.UrgentPrePassResult(List.of(), java.util.Set.of()));
        when(bundleDispatchOrchestrator.dispatchBundles(any(), any(), anyList(), anyList(), anyLong())).thenAnswer(inv -> {
            java.util.Map<Long, PendingOrder> orderById = inv.getArgument(1);
            List<AvailableCourier> couriers = inv.getArgument(2);
            return com.speedline.delivery.dispatch.engine.bundling.BundleAssignmentBatch.builder()
                    .assignments(List.of())
                    .remainingOrders(orderById == null ? List.of() : new java.util.ArrayList<>(orderById.values()))
                    .remainingCouriers(couriers)
                    .build();
        });
        when(costFunction.calculate(any(), any())).thenReturn(CostResult.builder()
                .feasible(true)
                .totalCost(5.0)
                .etaPickupMin(5)
                .etaDeliveryMin(20)
                .build());
    }
}
