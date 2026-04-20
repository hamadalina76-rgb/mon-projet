package com.speedline.delivery.dispatch.contract.mock;

import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * Day-1 mock solver: greedy lowest-cost courier per order.
 */
public class MockDispatchSolver implements DispatchSolver {

    @Override
    public List<Assignment> solve(CostMatrix matrix) {
        List<Assignment> output = new ArrayList<>();
        if (matrix == null || matrix.getOrders() == null || matrix.getCouriers() == null) return output;

        for (PendingOrder order : matrix.getOrders()) {
            AvailableCourier bestCourier = null;
            CostResult bestCost = null;

            for (AvailableCourier courier : matrix.getCouriers()) {
                CostResult current = matrix.get(order.getId(), courier.getId());
                if (current == null || !Boolean.TRUE.equals(current.getFeasible())) continue;

                if (bestCost == null || current.getTotalCost() < bestCost.getTotalCost()) {
                    bestCost = current;
                    bestCourier = courier;
                }
            }

            if (bestCourier != null && bestCost != null) {
                output.add(Assignment.builder()
                        .orderId(order.getId())
                        .courierId(bestCourier.getId())
                        .bundleId(null)
                        .cost(bestCost.getTotalCost())
                        .etaPickupMin(bestCost.getEtaPickupMin())
                        .etaDeliveryMin(bestCost.getEtaDeliveryMin())
                        .build());
            }
        }
        return output;
    }
}
