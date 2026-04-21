package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DISP-102: greedy lowest-cost solver.
 *
 * <p>Enumerates every feasible {@code (order, courier)} pair, sorts by
 * {@code totalCost} ascending, and walks the list assigning each pair whose
 * endpoints are still free. O((n·m) log(n·m)); sub-50 ms for ≤5 orders × 10 couriers.
 *
 * <p>Also used as the fallback path when {@link OrToolsSolver} is unavailable.
 */
@Slf4j
public class GreedySolver implements DispatchSolver {

    @Override
    public List<Assignment> solve(CostMatrix matrix) {
        if (matrix == null || matrix.getOrders() == null || matrix.getCouriers() == null) {
            return List.of();
        }
        final List<PendingOrder> orders = matrix.getOrders();
        final List<AvailableCourier> couriers = matrix.getCouriers();
        if (orders.isEmpty() || couriers.isEmpty()) return List.of();

        final List<Pair> feasible = new ArrayList<>(orders.size() * couriers.size());
        for (PendingOrder order : orders) {
            for (AvailableCourier courier : couriers) {
                CostResult c = matrix.get(order.getId(), courier.getId());
                if (c == null || !Boolean.TRUE.equals(c.getFeasible())) continue;
                if (c.getTotalCost() == null) continue;
                feasible.add(new Pair(order.getId(), courier.getId(), c));
            }
        }
        if (feasible.isEmpty()) return List.of();

        feasible.sort(Comparator.comparingDouble(p -> p.cost.getTotalCost()));

        final Set<Long> usedOrders = new HashSet<>();
        final Set<Long> usedCouriers = new HashSet<>();
        final List<Assignment> out = new ArrayList<>(Math.min(orders.size(), couriers.size()));

        for (Pair p : feasible) {
            if (usedOrders.contains(p.orderId) || usedCouriers.contains(p.courierId)) continue;
            usedOrders.add(p.orderId);
            usedCouriers.add(p.courierId);
            out.add(Assignment.builder()
                    .orderId(p.orderId)
                    .courierId(p.courierId)
                    .bundleId(null)
                    .cost(p.cost.getTotalCost())
                    .etaPickupMin(p.cost.getEtaPickupMin())
                    .etaDeliveryMin(p.cost.getEtaDeliveryMin())
                    .build());
            if (usedOrders.size() == orders.size() || usedCouriers.size() == couriers.size()) break;
        }
        log.debug("GreedySolver matched {} of {} orders", out.size(), orders.size());
        return out;
    }

    private record Pair(Long orderId, Long courierId, CostResult cost) {}
}
