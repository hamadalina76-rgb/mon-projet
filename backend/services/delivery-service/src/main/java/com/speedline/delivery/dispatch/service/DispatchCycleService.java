package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchMode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.event.producer.DeliveryEventProducer;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchCycleService {

    private static final String LOCK_PREFIX = "dispatch:lock:zone:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );

    private final DispatchZoneConfig zoneConfig;
    private final DispatchProperties dispatchProperties;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final CourierAvailabilityService courierAvailabilityService;
    private final CostFunction costFunction;
    private final DispatchSolver dispatchSolver;
    private final BundlingEngine bundlingEngine;
    private final PreAssignmentCalculator preAssignmentCalculator;
    private final EligibilityFilter eligibilityFilter;
    private final CourierResponseTimeoutTracker responseTimeoutTracker;
    private final DeliveryEventProducer deliveryEventProducer;
    private final DispatchMetrics dispatchMetrics;
    private final StringRedisTemplate redisTemplate;

    public void runCycle(Long zoneId) {
        if (zoneId == null) {
            return;
        }

        final DispatchMode mode = zoneConfig.getMode(zoneId);
        if (mode == DispatchMode.MANUAL) {
            return;
        }

        final String lockToken = UUID.randomUUID().toString();
        if (!tryAcquireZoneLock(zoneId, lockToken)) {
            return;
        }

        final Instant start = Instant.now();
        try {
            List<PendingOrder> pendingOrders = pendingOrderRedisRepository.findByZone(zoneId);
            if (pendingOrders.isEmpty()) {
                dispatchMetrics.recordCycle(zoneId, 0, 0, 0, 0, Duration.between(start, Instant.now()),
                        pendingOrderRedisRepository.countByZone(zoneId));
                return;
            }

            final int maxCapacity = Math.max(1, zoneConfig.getMaxCapacity(zoneId));
            if (pendingOrders.size() > maxCapacity) {
                pendingOrders = new ArrayList<>(pendingOrders.subList(0, maxCapacity));
            }

            final Map<Long, PendingOrder> orderById = new HashMap<>(pendingOrders.size());
            for (PendingOrder order : pendingOrders) {
                if (order != null && order.getId() != null) {
                    orderById.put(order.getId(), order);
                }
            }

            List<AvailableCourier> couriers = courierAvailabilityService.findOnlineByZone(zoneId);
            couriers = preAssignmentCalculator.enrichPreAssignable(couriers, zoneId, Clock.systemUTC());
            couriers = eligibilityFilter.selectPool(pendingOrders, couriers, zoneId, Clock.systemUTC());
            if (couriers.isEmpty()) {
                dispatchMetrics.recordCycle(zoneId, pendingOrders.size(), 0, 0, pendingOrders.size(),
                        Duration.between(start, Instant.now()), pendingOrderRedisRepository.countByZone(zoneId));
                return;
            }

            bundlingEngine.detectBundles(pendingOrders);

            final CostMatrix matrix = CostMatrix.builder()
                    .orders(pendingOrders)
                    .couriers(couriers)
                    .build();

            for (PendingOrder order : pendingOrders) {
                for (AvailableCourier courier : couriers) {
                    CostResult result = costFunction.calculate(order, courier);
                    if (result != null) {
                        matrix.put(order.getId(), courier.getId(), result);
                    }
                }
            }

            final List<Assignment> assignments = dispatchSolver.solve(matrix);
            int assigned = 0;
            for (Assignment assignment : assignments) {
                if (assignment == null || assignment.getOrderId() == null || assignment.getCourierId() == null) {
                    continue;
                }
                pendingOrderRedisRepository.remove(assignment.getOrderId());
                deliveryEventProducer.publishAssigned(DispatchAssignedEvent.builder()
                        .zoneId(zoneId)
                        .orderId(assignment.getOrderId())
                        .courierId(assignment.getCourierId())
                        .bundleId(assignment.getBundleId())
                        .cost(assignment.getCost())
                        .etaPickupMin(assignment.getEtaPickupMin())
                        .etaDeliveryMin(assignment.getEtaDeliveryMin())
                        .dispatchMode(mode)
                        .proposal(mode == DispatchMode.SEMI_AUTO)
                        .build());

                    PendingOrder pendingOrder = orderById.get(assignment.getOrderId());
                    responseTimeoutTracker.trackProposal(
                        assignment.getOrderId(),
                        assignment.getCourierId(),
                        Duration.ofSeconds(dispatchProperties.getResponseTimeout().getDeadlineSeconds()),
                        pendingOrder);

                    redisTemplate.opsForValue().set(
                        "courier:" + assignment.getCourierId() + ":lastAssignedAt",
                        Instant.now().toString(),
                        Duration.ofHours(24));
                assigned++;
            }

            final int unmatched = Math.max(0, pendingOrders.size() - assigned);
            dispatchMetrics.recordCycle(zoneId,
                    pendingOrders.size(),
                    couriers.size(),
                    assigned,
                    unmatched,
                    Duration.between(start, Instant.now()),
                    pendingOrderRedisRepository.countByZone(zoneId));
        } catch (Exception ex) {
            dispatchMetrics.recordError(zoneId, ex.getClass().getSimpleName());
            log.error("Dispatch cycle failed zoneId={}: {}", zoneId, ex.getMessage(), ex);
        } finally {
            releaseZoneLock(zoneId, lockToken);
        }
    }

    private boolean tryAcquireZoneLock(Long zoneId, String token) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey(zoneId),
                token,
                Duration.ofSeconds(dispatchProperties.getLock().getTtlSeconds()));
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseZoneLock(Long zoneId, String token) {
        redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                List.of(lockKey(zoneId)),
                Objects.toString(token, "")
        );
    }

    private static String lockKey(Long zoneId) {
        return LOCK_PREFIX + zoneId;
    }
}
