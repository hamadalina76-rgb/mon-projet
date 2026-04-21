package com.speedline.delivery.dispatch.impl.solver;

import com.speedline.delivery.dispatch.contract.engine.CostMatrix;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.CourierStatus;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AhmedVsKarimTest {

    @Test
    void ahmedWinsBecauseLowerCost() {
        GreedySolver solver = new GreedySolver();

        PendingOrder order = PendingOrder.builder().id(1L).build();
        AvailableCourier ahmed = AvailableCourier.builder().id(101L).status(CourierStatus.IDLE).build();
        AvailableCourier karim = AvailableCourier.builder().id(102L).status(CourierStatus.IDLE).build();

        CostMatrix m = new CostMatrix();
        m.setOrders(List.of(order));
        m.setCouriers(List.of(ahmed, karim));

        m.put(1L, 101L, CostResult.builder().feasible(true).totalCost(10.0).etaPickupMin(4).etaDeliveryMin(15).build());
        m.put(1L, 102L, CostResult.builder().feasible(true).totalCost(30.0).etaPickupMin(3).etaDeliveryMin(13).build());

        List<Assignment> out = solver.solve(m);

        assertEquals(1, out.size());
        assertEquals(101L, out.get(0).getCourierId());
    }
}
