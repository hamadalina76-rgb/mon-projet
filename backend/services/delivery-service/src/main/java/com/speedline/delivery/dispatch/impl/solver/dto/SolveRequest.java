package com.speedline.delivery.dispatch.impl.solver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolveRequest {

    @Builder.Default
    private String algorithm = "MCF";

    @Builder.Default
    private List<OrderNode> orders = new ArrayList<>();

    @Builder.Default
    private List<CourierNode> couriers = new ArrayList<>();

    @Builder.Default
    private List<List<Double>> costs = new ArrayList<>();

    private Integer timeoutMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderNode {
        private Long id;
        private Long partnerId;
        private Long customerId;

        private Double pickupLat;
        private Double pickupLon;
        private Double deliveryLat;
        private Double deliveryLon;

        private Integer guaranteedDeliveryMinutes;
        private Instant createdAt;
        private Boolean urgent;
        private Boolean largeOrder;
        private Boolean scheduled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierNode {
        private Long id;
        private Long zoneId;

        private Double lat;
        private Double lon;
        private String vehicleType;
        private Integer capacity;
    }
}
