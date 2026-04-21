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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreedySolverTest {

    private final GreedySolver solver = new GreedySolver();

    @Test
    void fiveOrdersThreeCouriers_threeAssignments_noDuplicates() {
        CostMatrix matrix = matrix(5, 3, false);

        List<Assignment> out = solver.solve(matrix);

        assertEquals(3, out.size());
        Set<Long> orders = out.stream().map(Assignment::getOrderId).collect(Collectors.toSet());
        Set<Long> couriers = out.stream().map(Assignment::getCourierId).collect(Collectors.toSet());
        assertEquals(3, orders.size());
        assertEquals(3, couriers.size());
    }

    @Test
    void underFiftyMs() {
        CostMatrix matrix = matrix(5, 3, false);
        assertTimeoutPreemptively(Duration.ofMillis(50), () -> solver.solve(matrix));
    }

    @Test
    void infeasibleSkipped() {
        CostMatrix matrix = matrix(5, 3, true);
        List<Assignment> out = solver.solve(matrix);
        assertTrue(out.isEmpty());
    }

    private static CostMatrix matrix(int orders, int couriers, boolean infeasible) {
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

        for (PendingOrder o : m.getOrders()) {
            for (AvailableCourier c : m.getCouriers()) {
                m.put(o.getId(), c.getId(), CostResult.builder()
                        .feasible(!infeasible)
                        .totalCost((double) ((o.getId() % 10) + (c.getId() % 10)))
                        .etaPickupMin(5)
                        .etaDeliveryMin(15)
                        .build());
            }
        }
        return m;
    }
}
