package com.speedline.delivery.dispatch.contract.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bundling detection output used by solver orchestration.
 * Contract-Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundlingResult {
    @Builder.Default
    private List<List<Long>> bundles = new ArrayList<>();

    @Builder.Default
    private Map<Long, Long> orderToBundleId = Map.of();

    @Builder.Default
    private Map<Long, List<Long>> bundleToOrders = Map.of();

    @Builder.Default
    private Map<Long, Integer> orderEtaMinutes = Map.of();
}
