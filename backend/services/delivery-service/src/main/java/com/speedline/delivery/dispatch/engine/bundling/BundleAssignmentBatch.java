package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.Builder;

import java.util.List;

@Builder
public record BundleAssignmentBatch(
        List<Assignment> assignments,
        List<PendingOrder> remainingOrders,
        List<AvailableCourier> remainingCouriers
) {
}
