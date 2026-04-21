package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * DISP-102: Hungarian wrapper for optimal bipartite matching.
 */
@Slf4j
public class HungarianSolver implements DispatchSolver {

    private static final double INF = HungarianAlgorithm.INF_COST_PLACEHOLDER;

    @Override
    public List<Assignment> solve(CostMatrix matrix) {
        if (matrix == null || matrix.getOrders() == null || matrix.getCouriers() == null) {
            return List.of();
        }

        final List<PendingOrder> orders = matrix.getOrders();
        final List<AvailableCourier> couriers = matrix.getCouriers();
        if (orders.isEmpty() || couriers.isEmpty()) {
            return List.of();
        }

        final int n = Math.max(orders.size(), couriers.size());
        final double[][] costs = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                costs[i][j] = INF;
            }
        }

        for (int i = 0; i < orders.size(); i++) {
            PendingOrder order = orders.get(i);
            for (int j = 0; j < couriers.size(); j++) {
                AvailableCourier courier = couriers.get(j);
                CostResult c = matrix.get(order.getId(), courier.getId());
                if (c == null || !Boolean.TRUE.equals(c.getFeasible()) || c.getTotalCost() == null) {
                    costs[i][j] = INF;
                } else {
                    costs[i][j] = c.getTotalCost();
                }
            }
        }

        int[] rowToCol = HungarianAlgorithm.solve(costs);

        List<Assignment> out = new ArrayList<>(Math.min(orders.size(), couriers.size()));
        for (int i = 0; i < orders.size(); i++) {
            int col = rowToCol[i];
            if (col < 0 || col >= couriers.size()) {
                continue;
            }
            if (costs[i][col] >= INF / 2) {
                continue;
            }

            PendingOrder order = orders.get(i);
            AvailableCourier courier = couriers.get(col);
            CostResult c = matrix.get(order.getId(), courier.getId());
            if (c == null) {
                continue;
            }

            out.add(Assignment.builder()
                    .orderId(order.getId())
                    .courierId(courier.getId())
                    .bundleId(null)
                    .cost(c.getTotalCost())
                    .etaPickupMin(c.getEtaPickupMin())
                    .etaDeliveryMin(c.getEtaDeliveryMin())
                    .build());
        }

        log.debug("HungarianSolver matched {} of {} orders", out.size(), orders.size());
        return out;
    }
}
