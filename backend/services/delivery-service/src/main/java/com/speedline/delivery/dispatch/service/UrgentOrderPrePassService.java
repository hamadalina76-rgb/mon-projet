package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrgentOrderPrePassService {

    private final DeviationCalculator deviationCalculator;
    private final CostFunction costFunction;
    private final DispatchSolver dispatchSolver;
    private final DispatchMetrics dispatchMetrics;

    public record UrgentPrePassResult(List<Assignment> assignments, Set<Long> consumedOrderIds) {
        static UrgentPrePassResult empty() {
            return new UrgentPrePassResult(List.of(), Set.of());
        }
    }

    public UrgentPrePassResult runPrePass(List<PendingOrder> pendingOrders,
                                          List<AvailableCourier> allOnlineCouriers,
                                          List<AvailableCourier> eligiblePool,
                                          Long zoneId) {
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return UrgentPrePassResult.empty();
        }

        List<PendingOrder> urgentOrders = pendingOrders.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsUrgent()))
                .toList();
        if (urgentOrders.isEmpty()) {
            return UrgentPrePassResult.empty();
        }

        Set<Long> eligibleIds = (eligiblePool == null ? List.<AvailableCourier>of() : eligiblePool)
                .stream()
                .map(AvailableCourier::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<AvailableCourier> deviationEligible = (allOnlineCouriers == null ? List.<AvailableCourier>of() : allOnlineCouriers)
                .stream()
                .filter(c -> c != null && c.getId() != null)
                .filter(c -> !eligibleIds.contains(c.getId()))
                .filter(c -> c.getStatus() == CourierStatus.ON_DELIVERY)
                .filter(c -> urgentOrders.stream().anyMatch(o ->
                        o.getPartnerLat() != null
                                && o.getPartnerLon() != null
                                && deviationCalculator.isDeviationEligible(c, o.getPartnerLat(), o.getPartnerLon())))
                .toList();

        dispatchMetrics.recordUrgentDeviationEligible(zoneId, deviationEligible.size());

        List<AvailableCourier> expandedPool = new ArrayList<>(eligiblePool == null ? List.of() : eligiblePool);
        expandedPool.addAll(deviationEligible);
        if (expandedPool.isEmpty()) {
            return UrgentPrePassResult.empty();
        }

        CostMatrix matrix = CostMatrix.builder().orders(urgentOrders).couriers(expandedPool).build();
        for (PendingOrder order : urgentOrders) {
            for (AvailableCourier courier : expandedPool) {
                CostResult result = costFunction.calculate(order, courier);
                if (result != null) {
                    matrix.put(order.getId(), courier.getId(), result);
                }
            }
        }

        List<Assignment> assignments = dispatchSolver.solve(matrix);
        Set<Long> consumed = assignments.stream()
                .filter(a -> a != null && a.getOrderId() != null)
                .map(Assignment::getOrderId)
                .collect(Collectors.toSet());

        dispatchMetrics.recordUrgentPrePass(zoneId, urgentOrders.size(), assignments.size());
        return new UrgentPrePassResult(assignments, consumed);
    }
}
