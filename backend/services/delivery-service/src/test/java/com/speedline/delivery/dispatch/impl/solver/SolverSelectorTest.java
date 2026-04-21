package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolverSelectorTest {

    @Mock
    private GreedySolver greedySolver;
    @Mock
    private HungarianSolver hungarianSolver;
    @Mock
    private OrToolsSolver orToolsSolver;

    private SolverSelector selector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        DispatchProperties properties = new DispatchProperties();
        DispatchProperties.Solver solver = new DispatchProperties.Solver();
        solver.setGreedyMaxOrders(5);
        solver.setHungarianMaxOrders(10);
        DispatchProperties.Solver.OrTools orTools = new DispatchProperties.Solver.OrTools();
        orTools.setEnabled(true);
        solver.setOrtools(orTools);
        properties.setSolver(solver);

        selector = new SolverSelector(properties, greedySolver, hungarianSolver, orToolsSolver);

        when(greedySolver.solve(any())).thenReturn(List.of());
        when(hungarianSolver.solve(any())).thenReturn(List.of());
        when(orToolsSolver.solveAsync(any())).thenReturn(CompletableFuture.completedFuture(List.of()));
    }

    @Test
    void routesByOrderCount() {
        selector.solve(matrixWithOrders(3));
        verify(greedySolver).solve(any());

        selector.solve(matrixWithOrders(7));
        verify(hungarianSolver).solve(any());

        selector.solve(matrixWithOrders(15));
        verify(orToolsSolver).solveAsync(any());
    }

    @Test
    void orToolsFallsBackToGreedy() {
        when(orToolsSolver.solveAsync(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));

        selector.solve(matrixWithOrders(15));

        verify(orToolsSolver).solveAsync(any());
        verify(greedySolver).solve(any());
        verify(hungarianSolver, never()).solve(any());
    }

    @Test
    void bundleHintRoutesToOrToolsEvenForSmallBatch() {
        CostMatrix m = matrixWithOrders(3);
        m.setOrders(List.of(
                PendingOrder.builder().id(1L).isLargeOrder(true).build(),
                PendingOrder.builder().id(2L).isLargeOrder(false).build(),
                PendingOrder.builder().id(3L).isLargeOrder(false).build()
        ));

        selector.solve(m);

        verify(orToolsSolver).solveAsync(any());
        verify(greedySolver, never()).solve(any());
        verify(hungarianSolver, never()).solve(any());
    }

    private static CostMatrix matrixWithOrders(int count) {
        CostMatrix m = new CostMatrix();
        List<PendingOrder> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orders.add(PendingOrder.builder().id((long) (i + 1)).build());
        }
        m.setOrders(orders);
        m.setCouriers(List.of());
        return m;
    }
}
