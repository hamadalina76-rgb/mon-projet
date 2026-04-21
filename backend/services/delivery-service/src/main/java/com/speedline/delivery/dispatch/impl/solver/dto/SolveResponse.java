package com.speedline.delivery.dispatch.impl.solver.dto;

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
public class SolveResponse {

    @Builder.Default
    private String status = "ok";

    @Builder.Default
    private String solverUsed = "MCF";

    @Builder.Default
    private List<AssignmentOut> assignments = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentOut {
        private Long orderId;
        private Long courierId;
        private Long bundleId;

        private Double cost;
        private Integer etaPickupMin;
        private Integer etaDeliveryMin;
    }
}
