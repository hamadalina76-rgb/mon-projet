package com.speedline.delivery.dispatch.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared dispatch output assignment object.
 * Contract-Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    private Long orderId;
    private Long courierId;
    private Long bundleId;

    private Double cost;
    private Integer etaPickupMin;
    private Integer etaDeliveryMin;
}
