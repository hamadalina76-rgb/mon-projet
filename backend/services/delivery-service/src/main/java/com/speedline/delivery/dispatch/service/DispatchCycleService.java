package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.runtime.RuntimeDispatchTuningService;
import com.speedline.delivery.dispatch.config.service.DispatchCycleCaptureRecorder;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.engine.bundling.BundleAssignmentBatch;
import com.speedline.delivery.dispatch.engine.bundling.BundleDispatchOrchestrator;
import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
import com.speedline.delivery.websocket.TrackingWebSocketHandler;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchCycleService {

    private static final String LOCK_PREFIX = "dispatch:lock:zone:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final DispatchZoneConfig zoneConfig;
    private final DispatchProperties dispatchProperties;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final CourierAvailabilityService courierAvailabilityService;
    private final CostFunction costFunction;
    private final DispatchSolver dispatchSolver;
    private final BundlingEngine bundlingEngine;
    private final BundleDispatchOrchestrator bundleDispatchOrchestrator;
    private final PreAssignmentCalculator preAssignmentCalculator;
    private final EligibilityFilter eligibilityFilter;
    private final UrgentOrderPrePassService urgentOrderPrePassService;
    private final UrgentBonusService urgentBonusService;
    private final CourierResponseTimeoutTracker responseTimeoutTracker;
    private final DeliveryEventProducer deliveryEventProducer;
    private final DispatchMetrics dispatchMetrics;
    private final DispatchRealtimePublisher dispatchRealtimePublisher;
    private final StringRedisTemplate redisTemplate;
    private final RuntimeDispatchTuningService runtimeDispatchTuningService;
    private final DispatchCycleCaptureRecorder cycleCaptureRecorder;
    private final DispatchProposalService dispatchProposalService;
    private final DispatchDeliveryRecordService dispatchDeliveryRecordService;
    private final OrderServiceClient orderServiceClient;
    private final PendingOrderEnricher pendingOrderEnricher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TrackingWebSocketHandler trackingWebSocketHandler;

    public void runCycle(Long zoneId) {
        if (zoneId == null) return;
        DispatchMode mode = zoneConfig.getMode(zoneId);
        if (mode == DispatchMode.MANUAL) return;

        String token = UUID.randomUUID().toString();
        if (!tryAcquireZoneLock(zoneId, token)) return;

        Instant start = Instant.now();
        try {
            List<PendingOrder> pendingOrders = pendingOrderRedisRepository.findByZone(zoneId);
            if (pendingOrders.isEmpty()) {
                pendingOrders = hydratePendingOrdersFromOrderService(zoneId);
            }
            pendingOrders = pendingOrders.stream()
                    .filter(o -> o != null && o.getId() != null && !dispatchProposalService.hasPendingProposal(o.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (pendingOrders.isEmpty()) {
                dispatchMetrics.recordCycle(zoneId, 0, 0, 0, 0, Duration.between(start, Instant.now()),
                        pendingOrderRedisRepository.countByZone(zoneId));
                dispatchRealtimePublisher.publishZoneCycle(zoneId, 0, 0, 0, 0);
                dispatchMetrics.recordCycle(zoneId, 0, 0, 0, 0, Duration.between(start, Instant.now()), pendingOrderRedisRepository.countByZone(zoneId));
                return;
            }

            int maxCapacity = Math.max(1, zoneConfig.getMaxCapacity(zoneId));
            if (pendingOrders.size() > maxCapacity) {
                pendingOrders = new ArrayList<>(pendingOrders.subList(0, maxCapacity));
            }

            Map<Long, PendingOrder> orderById = new HashMap<>();
            for (PendingOrder order : pendingOrders) {
                if (order != null && order.getId() != null) orderById.put(order.getId(), order);
            }

            List<AvailableCourier> couriers = courierAvailabilityService.findOnlineByZone(zoneId);
            couriers = preAssignmentCalculator.enrichPreAssignable(couriers, zoneId, Clock.systemUTC());
            EligibilityPool pool = eligibilityFilter.selectPoolDetailed(pendingOrders, couriers, zoneId, Clock.systemUTC());
            couriers = pool.pool();
            cycleCaptureRecorder.captureSnapshot(
                    zoneId,
                    new ArrayList<>(pendingOrders),
                    new ArrayList<>(couriers),
                    !pool.internalOnly().isEmpty());
            if (couriers.isEmpty()) {
                dispatchMetrics.recordCycle(zoneId, pendingOrders.size(), 0, 0, pendingOrders.size(),
                        Duration.between(start, Instant.now()), pendingOrderRedisRepository.countByZone(zoneId));
                dispatchRealtimePublisher.publishZoneCycle(zoneId, pendingOrders.size(), 0, 0, pendingOrders.size());
                dispatchMetrics.recordCycle(zoneId, pendingOrders.size(), 0, 0, pendingOrders.size(), Duration.between(start, Instant.now()), pendingOrderRedisRepository.countByZone(zoneId));
                return;
            }

                List<AvailableCourier> allOnlineCouriers = courierAvailabilityService.findOnlineByZone(zoneId);
                UrgentOrderPrePassService.UrgentPrePassResult urgentResult =
                    urgentOrderPrePassService.runPrePass(pendingOrders, allOnlineCouriers, couriers, zoneId);
                int assigned = publishAssignments(urgentResult.assignments(), mode, zoneId, orderById);
                urgentResult.assignments().stream()
                    .filter(a -> a != null && a.getCourierId() != null && a.getOrderId() != null)
                    .forEach(a -> urgentBonusService.creditBonus(a.getCourierId(), a.getOrderId()));
                if (!urgentResult.consumedOrderIds().isEmpty()) {
                pendingOrders = pendingOrders.stream()
                    .filter(o -> !urgentResult.consumedOrderIds().contains(o.getId()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                urgentResult.consumedOrderIds().forEach(orderById::remove);
                couriers = couriers.stream()
                    .filter(c -> urgentResult.assignments().stream()
                        .noneMatch(a -> a != null && c.getId().equals(a.getCourierId())))
                    .toList();
                }

            BundlingResult bundling = bundlingEngine.detectBundles(pendingOrders);
            BundleAssignmentBatch bundleBatch = bundleDispatchOrchestrator.dispatchBundles(
                    bundling,
                    orderById,
                    couriers,
                    pool.internalOnly(),
                    zoneId);
                assigned += publishAssignments(bundleBatch.assignments(), mode, zoneId, orderById);

            List<PendingOrder> remainingOrders = new ArrayList<>(bundleBatch.remainingOrders());
            List<AvailableCourier> remainingCouriers = new ArrayList<>(bundleBatch.remainingCouriers());
            if (!remainingOrders.isEmpty() && !remainingCouriers.isEmpty()) {
                CostMatrix matrix = CostMatrix.builder().orders(remainingOrders).couriers(remainingCouriers).build();
                for (PendingOrder order : remainingOrders) {
                    for (AvailableCourier courier : remainingCouriers) {
                        CostResult result = costFunction.calculate(order, courier);
                        if (result != null) matrix.put(order.getId(), courier.getId(), result);
                    }
                }
                assigned += publishAssignments(dispatchSolver.solve(matrix), mode, zoneId, orderById);
            }

            int unmatched = Math.max(0, orderById.size() - assigned);
            dispatchMetrics.recordCycle(zoneId, orderById.size(), couriers.size(), assigned, unmatched, Duration.between(start, Instant.now()), pendingOrderRedisRepository.countByZone(zoneId));
        } catch (Exception ex) {
            dispatchMetrics.recordError(zoneId, ex.getClass().getSimpleName());
            log.error("Dispatch cycle failed zoneId={}: {}", zoneId, ex.getMessage(), ex);
        } finally {
            releaseZoneLock(zoneId, token);
        }
    }

    private int publishAssignments(List<Assignment> assignments, DispatchMode mode, Long zoneId, Map<Long, PendingOrder> orderById) {
        if (assignments == null || assignments.isEmpty()) return 0;
        if (mode == DispatchMode.SEMI_AUTO) {
            int published = 0;
            for (Assignment assignment : assignments) {
                if (assignment == null || assignment.getOrderId() == null || assignment.getCourierId() == null) {
                    continue;
                }
                if (dispatchProposalService.hasPendingProposal(assignment.getOrderId())) {
                    continue;
                }
                dispatchProposalService.enqueueProposal(zoneId, assignment, orderById);
                published++;
            }
            return published;
        }
        int published = 0;
        for (Assignment assignment : assignments) {
            if (assignment == null || assignment.getOrderId() == null || assignment.getCourierId() == null) continue;
            try {
                orderServiceClient.assignCourierToOrder(
                        assignment.getOrderId(), Map.of("courierId", assignment.getCourierId()));
            } catch (FeignException e) {
                log.warn("Auto dispatch: order-service assign failed orderId={} status={} — skipping this assignment",
                        assignment.getOrderId(), e.status());
                continue;
            }
            pendingOrderRedisRepository.remove(assignment.getOrderId());
            PendingOrder pendingForDb = orderById.get(assignment.getOrderId());
            dispatchDeliveryRecordService.createFromAssignment(pendingForDb, assignment);
            pushDeliveryOfferToWs(assignment, pendingForDb, mode.name());
            deliveryEventProducer.publishAssigned(DispatchAssignedEvent.builder()
                    .zoneId(zoneId)
                    .orderId(assignment.getOrderId())
                    .courierId(assignment.getCourierId())
                    .bundleId(assignment.getBundleId())
                    .cost(assignment.getCost())
                    .etaPickupMin(assignment.getEtaPickupMin())
                    .etaDeliveryMin(assignment.getEtaDeliveryMin())
                    .dispatchMode(mode)
                    .proposal(false)
                    .build());

            PendingOrder pendingOrder = orderById.get(assignment.getOrderId());
            responseTimeoutTracker.trackProposal(
                    assignment.getOrderId(),
                    assignment.getCourierId(),
                    Duration.ofSeconds(runtimeDispatchTuningService.responseTimeoutDeadlineSeconds()),
                    pendingOrder);
            redisTemplate.opsForValue().set(
                    "courier:" + assignment.getCourierId() + ":lastAssignedAt",
                    Instant.now().toString(),
                    Duration.ofHours(24));
            published++;
        }
        return published;
    }

    private boolean tryAcquireZoneLock(Long zoneId, String token) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey(zoneId), token, Duration.ofSeconds(runtimeDispatchTuningService.lockTtlSeconds()));
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseZoneLock(Long zoneId, String token) {
        redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey(zoneId)), Objects.toString(token, ""));
    }

    private static String lockKey(Long zoneId) {
        return LOCK_PREFIX + zoneId;
    }

    private void pushDeliveryOfferToWs(Assignment assignment, PendingOrder order, String dispatchMode) {
        if (trackingWebSocketHandler == null || assignment.getCourierId() == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", assignment.getOrderId());
            payload.put("dispatchMode", dispatchMode);
            payload.put("etaPickupMin", assignment.getEtaPickupMin());
            payload.put("etaDeliveryMin", assignment.getEtaDeliveryMin());
            payload.put("offeredAt", Instant.now().toString());
            if (order != null) {
                payload.put("orderNumber", order.getOrderNumber());
                payload.put("partnerName", order.getPartnerName());
                payload.put("pickupAddress", order.getPickupAddress());
                payload.put("dropoffAddress", order.getDropoffAddress());
                payload.put("deliveryFee", order.getDeliveryFee() != null ? order.getDeliveryFee().doubleValue() : 0.0);
                payload.put("isUrgent", Boolean.TRUE.equals(order.getIsUrgent()));
                payload.put("partnerLat", order.getPartnerLat());
                payload.put("partnerLon", order.getPartnerLon());
                payload.put("customerLat", order.getCustomerLat());
                payload.put("customerLon", order.getCustomerLon());
            }
            trackingWebSocketHandler.sendToCourier(String.valueOf(assignment.getCourierId()), "DELIVERY_OFFER", payload);
        } catch (Exception ex) {
            log.warn("Could not push DELIVERY_OFFER to courier {}: {}", assignment.getCourierId(), ex.getMessage());
        }
    }

    /**
     * Fallback de résilience : si la queue Redis est vide (ex. consommateur Pub/Sub indisponible),
     * hydrate depuis order-service pour éviter un pool à 0 commande.
     */
    private List<PendingOrder> hydratePendingOrdersFromOrderService(Long zoneId) {
        try {
            List<java.util.Map<String, Object>> awaiting = orderServiceClient.getOrdersAwaitingCourier();
            if (awaiting == null || awaiting.isEmpty()) {
                return List.of();
            }
            List<PendingOrder> recovered = awaiting.stream()
                    .map(pendingOrderEnricher::enrichOptional)
                    .flatMap(Optional::stream)
                    .filter(o -> o.getZoneId() != null && o.getZoneId().equals(zoneId))
                    .collect(Collectors.toList());
            recovered.forEach(pendingOrderRedisRepository::add);
            return recovered;
        } catch (Exception ex) {
            log.warn("Unable to hydrate pending orders from order-service for zone {}: {}", zoneId, ex.getMessage());
            return List.of();
        }
    }
}
