package com.speedline.delivery.dispatch.engine.bundling;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record BundleRoute(
        List<Stop> stops,
        Map<Long, Integer> etaPickupMinByOrder,
        Map<Long, Integer> etaDeliveryMinByOrder
) {

    @Builder
    public record Stop(
            Long orderId,
            String type,
            Double lat,
            Double lon,
            Integer etaMin
    ) {
    }
}
