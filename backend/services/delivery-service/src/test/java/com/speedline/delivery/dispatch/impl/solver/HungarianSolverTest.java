package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungarianSolverTest {

    private final HungarianSolver solver = new HungarianSolver();

    @Test
    void tenByTen_optimal_underTwoHundredMs() {
        CostMatrix matrix = identityLike(10, 10);

        List<Assignment> out = assertTimeoutPreemptively(Duration.ofMillis(200), () -> solver.solve(matrix));

        assertEquals(10, out.size());
        double total = out.stream().mapToDouble(a -> a.getCost() == null ? 0.0 : a.getCost()).sum();
        assertEquals(10.0, total, 0.0001);
    }

    @Test
    void unbalanced_5orders_10couriers() {
        CostMatrix matrix = identityLike(5, 10);
        List<Assignment> out = solver.solve(matrix);
        assertEquals(5, out.size());
    }

    @Test
    void infeasiblePadded() {
        CostMatrix matrix = identityLike(5, 5);
        // Make one complete row infeasible -> at most 4 matches.
        Long blockedOrder = matrix.getOrders().get(0).getId();
        for (AvailableCourier c : matrix.getCouriers()) {
            matrix.put(blockedOrder, c.getId(), CostResult.builder().feasible(false).totalCost(999999.0).build());
        }

        List<Assignment> out = solver.solve(matrix);
        assertTrue(out.size() <= 4);
    }

    private static CostMatrix identityLike(int orders, int couriers) {
        CostMatrix m = new CostMatrix();
        for (int i = 0; i < orders; i++) {
            m.getOrders().add(PendingOrder.builder().id((long) (i + 1)).build());
        }
        for (int j = 0; j < couriers; j++) {
            m.getCouriers().add(AvailableCourier.builder()
                    .id((long) (100 + j))
                    .status(CourierStatus.IDLE)
                    .build());
        }

        for (int i = 0; i < m.getOrders().size(); i++) {
            for (int j = 0; j < m.getCouriers().size(); j++) {
                double cost = (i == j) ? 1.0 : 100.0 + i + j;
                m.put(m.getOrders().get(i).getId(), m.getCouriers().get(j).getId(),
                        CostResult.builder().feasible(true).totalCost(cost).etaPickupMin(5).etaDeliveryMin(12).build());
            }
        }
        return m;
    }
}
