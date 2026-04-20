package com.speedline.delivery.dispatch.contract.engine;

import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;

/**
 * Shared contract to calculate assignment cost for one order and one courier.
 * Contract-Version: 1.0
 */
public interface CostFunction {

    CostResult calculate(PendingOrder order, AvailableCourier courier);
}
