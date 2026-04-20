package com.speedline.delivery.dispatch.contract.mock;

import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;

/**
 * Day-1 mock implementation to unblock integration without business scoring.
 */
public class MockCostFunction implements CostFunction {

    @Override
    public CostResult calculate(PendingOrder order, AvailableCourier courier) {
        double score = 10.0;
        if (Boolean.TRUE.equals(order.getIsUrgent())) score -= 1.0;
        if (courier.getRating() != null) score -= Math.min(2.0, courier.getRating() / 3.0);

        return CostResult.builder()
                .totalCost(Math.max(1.0, score))
                .etaPickupMin(10)
                .etaDeliveryMin(order.getGuaranteedDeliveryMinutes() != null
                        ? order.getGuaranteedDeliveryMinutes()
                        : 25)
                .feasible(true)
                .reason("MOCK")
                .build();
    }
}
