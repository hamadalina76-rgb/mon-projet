package com.speedline.delivery.dispatch.contract.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cost function output for a (order, courier) pair.
 * Contract-Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostResult {
    private Double totalCost;
    private Integer etaPickupMin;
    private Integer etaDeliveryMin;
    private Boolean feasible;
    private String reason;
}
