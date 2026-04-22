package com.speedline.delivery.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchProposalView {
    private Long zoneId;
    private Long orderId;
    private Long courierId;
    private Long bundleId;
    private Double cost;
    private Integer etaPickupMin;
    private Integer etaDeliveryMin;
    private Instant createdAt;
}
