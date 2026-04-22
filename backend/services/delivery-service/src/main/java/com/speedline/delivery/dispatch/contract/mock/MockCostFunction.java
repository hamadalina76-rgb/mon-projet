package com.speedline.delivery.dispatch.contract.mock;

import com.speedline.delivery.dispatch.contract.engine.CostFunction;
import com.speedline.delivery.dispatch.contract.engine.CostResult;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;

import java.time.Duration;
import java.time.Instant;

/**
 * Day-1 mock implementation to unblock integration without business scoring.
 */
public class MockCostFunction implements CostFunction {

    private final double preAssignCostPenalty;

    public MockCostFunction() {
        this(1.2);
    }

    public MockCostFunction(double preAssignCostPenalty) {
        this.preAssignCostPenalty = preAssignCostPenalty > 0 ? preAssignCostPenalty : 1.2;
    }

    @Override
    public CostResult calculate(PendingOrder order, AvailableCourier courier) {
        double score = 10.0;
        if (Boolean.TRUE.equals(order.getIsUrgent())) score -= 80.0;
        if (courier.getRating() != null) score -= Math.min(2.0, courier.getRating() / 3.0);

        int etaPickupMin = 10;
        if (courier.getAvailableFromInstant() != null) {
            long waitMinutes = Math.max(0L, Duration.between(Instant.now(), courier.getAvailableFromInstant()).toMinutes());
            etaPickupMin += (int) waitMinutes;
            score *= preAssignCostPenalty;
        }

        return CostResult.builder()
                .totalCost(Math.max(1.0, score))
                .etaPickupMin(etaPickupMin)
                .etaDeliveryMin(order.getGuaranteedDeliveryMinutes() != null
                        ? order.getGuaranteedDeliveryMinutes()
                        : 25)
                .feasible(true)
                .reason("MOCK")
                .build();
    }
}
