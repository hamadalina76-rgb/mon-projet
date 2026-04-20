package com.speedline.delivery.dispatch.contract.engine;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;

import java.util.List;

/**
 * Shared contract to detect bundle opportunities across pending orders.
 * Contract-Version: 1.0
 */
public interface BundlingEngine {

    BundlingResult detectBundles(List<PendingOrder> orders);
}
