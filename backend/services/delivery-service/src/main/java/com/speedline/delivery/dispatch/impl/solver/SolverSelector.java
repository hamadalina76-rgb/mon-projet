package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.DispatchSolver;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DISP-102: primary dispatch solver that routes to Greedy/Hungarian/OR-Tools by problem size.
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class SolverSelector implements DispatchSolver {

    private final DispatchProperties properties;
    private final GreedySolver greedySolver;
    private final HungarianSolver hungarianSolver;
    private final OrToolsSolver orToolsSolver;

    @Override
    public List<Assignment> solve(CostMatrix matrix) {
        List<PendingOrder> orders = matrix == null || matrix.getOrders() == null ? List.of() : matrix.getOrders();
        if (orders.isEmpty()) {
            return List.of();
        }

        int orderCount = orders.size();
        int greedyMax = properties.getSolver().getGreedyMaxOrders();
        int hungarianMax = properties.getSolver().getHungarianMaxOrders();
        boolean hasBundleHint = orders.stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsLargeOrder()));

        // Ticket rule: bundles must be routed to OR-Tools regardless of small batch size.
        if (hasBundleHint) {
            if (!properties.getSolver().getOrtools().isEnabled()) {
                log.debug("SolverSelector routed to GreedySolver because OR-Tools is disabled (bundle hint)");
                return greedySolver.solve(matrix);
            }
            try {
                log.debug("SolverSelector routed to OrToolsSolver for {} orders (bundle hint)", orderCount);
                return orToolsSolver.solveAsync(matrix).get();
            } catch (Exception ex) {
                log.warn("OrToolsSolver failed in selector (bundle hint), fallback to Greedy: {}", ex.getMessage());
                return greedySolver.solve(matrix);
            }
        }

        if (orderCount <= greedyMax) {
            log.debug("SolverSelector routed to GreedySolver for {} orders", orderCount);
            return greedySolver.solve(matrix);
        }

        if (orderCount <= hungarianMax && !hasBundleHint) {
            log.debug("SolverSelector routed to HungarianSolver for {} orders", orderCount);
            return hungarianSolver.solve(matrix);
        }

        if (!properties.getSolver().getOrtools().isEnabled()) {
            log.debug("SolverSelector routed to GreedySolver because OR-Tools is disabled");
            return greedySolver.solve(matrix);
        }

        try {
            log.debug("SolverSelector routed to OrToolsSolver for {} orders", orderCount);
            return orToolsSolver.solveAsync(matrix).get();
        } catch (Exception ex) {
            log.warn("OrToolsSolver failed in selector, fallback to Greedy: {}", ex.getMessage());
            return greedySolver.solve(matrix);
        }
    }
}
