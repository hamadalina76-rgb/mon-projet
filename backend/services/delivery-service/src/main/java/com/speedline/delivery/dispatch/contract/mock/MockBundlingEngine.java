package com.speedline.delivery.dispatch.contract.mock;

import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;

import java.util.List;
import java.util.Map;

/**
 * Day-1 mock implementation that disables bundling by default.
 */
public class MockBundlingEngine implements BundlingEngine {

    @Override
    public BundlingResult detectBundles(List<PendingOrder> orders) {
        return BundlingResult.builder()
                .bundles(List.of())
                .orderToBundleId(Map.of())
                .build();
    }
}
