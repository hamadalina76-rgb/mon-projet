package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.DispatchZoneConfig;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.CourierType;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityFilter {

    private final DispatchProperties properties;
    private final DispatchZoneConfig zoneConfig;
    private final DispatchMetrics dispatchMetrics;

    public List<AvailableCourier> selectPool(List<PendingOrder> orders,
                                             List<AvailableCourier> couriers,
                                             Long zoneId,
                                             Clock clock) {
        if (couriers == null || couriers.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now(clock);
        LocalTime nowLocal = LocalTime.now(clock);

        List<AvailableCourier> internalPool = couriers.stream()
                .filter(c -> c.getType() == CourierType.INTERNAL)
                .filter(this::isDispatchableStatus)
                .filter(c -> isInShift(c, nowLocal))
                .toList();

        List<String> reasons = new ArrayList<>();
        if (internalPool.isEmpty()) {
            reasons.add("no_internal");
        }

        if (hasWaitingOrder(orders, now)) {
            reasons.add("order_waiting");
        }

        int shortageThreshold = zoneConfig.getInternalShortageThreshold(zoneId);
        if (internalPool.size() < shortageThreshold) {
            reasons.add("shortage");
        }

        if (reasons.isEmpty()) {
            dispatchMetrics.recordEligibilityPhase1Only(zoneId);
            return internalPool;
        }

        reasons.forEach(reason -> dispatchMetrics.recordEligibilityPhase2Triggered(zoneId, reason));
        List<AvailableCourier> externals = couriers.stream()
                .filter(c -> c.getType() == CourierType.EXTERNAL)
                .filter(this::isDispatchableStatus)
                .toList();

        List<AvailableCourier> combined = new ArrayList<>(internalPool.size() + externals.size());
        combined.addAll(internalPool);
        combined.addAll(externals);
        return combined;
    }

    private boolean hasWaitingOrder(List<PendingOrder> orders, Instant now) {
        if (orders == null || orders.isEmpty()) return false;
        long threshold = properties.getEligibility().getOrderWaitingThresholdSeconds();
        return orders.stream()
                .map(PendingOrder::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .anyMatch(createdAt -> createdAt.plusSeconds(threshold).isBefore(now));
    }

    private boolean isDispatchableStatus(AvailableCourier courier) {
        return courier != null
                && (courier.getStatus() == CourierStatus.IDLE || courier.getStatus() == CourierStatus.PRE_ASSIGNABLE);
    }

    public static boolean isInShift(AvailableCourier courier, LocalTime now) {
        if (courier == null || courier.getShiftStart() == null || courier.getShiftEnd() == null) {
            return true;
        }
        LocalTime start = courier.getShiftStart();
        LocalTime end = courier.getShiftEnd();
        if (!end.isBefore(start)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }
}
