package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;

import java.util.List;

public interface BundleRouter {

    BundleRoute route(AvailableCourier courier, List<PendingOrder> bundleOrders);
}
