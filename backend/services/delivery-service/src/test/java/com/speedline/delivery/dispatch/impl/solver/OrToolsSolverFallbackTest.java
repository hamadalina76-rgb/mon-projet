package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class OrToolsSolverFallbackTest {

    @Mock
    private SolverServiceClient solverServiceClient;

    private OrToolsSolver solver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DispatchProperties props = new DispatchProperties();
        DispatchProperties.Solver s = new DispatchProperties.Solver();
        DispatchProperties.Solver.OrTools o = new DispatchProperties.Solver.OrTools();
        o.setEnabled(true);
        o.setTimeoutMs(2000);
        o.setUrl("http://localhost:5001");
        s.setOrtools(o);
        props.setSolver(s);
        solver = new OrToolsSolver(solverServiceClient, props, new GreedySolver());
    }

    @Test
    void httpError_returnsGreedyResult() {
        CostMatrix matrix = new CostMatrix();
        PendingOrder order = PendingOrder.builder().id(1L).build();
        AvailableCourier courier = AvailableCourier.builder().id(10L).status(CourierStatus.IDLE).build();
        matrix.setOrders(List.of(order));
        matrix.setCouriers(List.of(courier));
        matrix.put(1L, 10L, CostResult.builder().feasible(true).totalCost(10.0).etaPickupMin(5).etaDeliveryMin(12).build());

        List<Assignment> out = solver.fallbackAsync(matrix, new RuntimeException("HTTP 500")).join();

        assertEquals(1, out.size());
        assertEquals(1L, out.get(0).getOrderId());
        assertEquals(10L, out.get(0).getCourierId());
    }
}
