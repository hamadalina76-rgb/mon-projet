package com.speedline.delivery.dispatch.impl.bundling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleRouteRequest {

    private Double startLat;
    private Double startLon;

    @Builder.Default
    private List<Stop> stops = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stop {
        private Long orderId;
        private Double lat;
        private Double lon;
    }
}
