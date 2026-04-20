package com.speedline.delivery.dispatch.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Shared order input object for dispatching contract.
 * Contract-Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrder {
    private Long id;
    private Long partnerId;
    private Long customerId;

    private Double partnerLat;
    private Double partnerLon;
    private Double customerLat;
    private Double customerLon;

    private Long zoneId;
    private Integer guaranteedDeliveryMinutes;

    private Boolean isUrgent;
    private Boolean isLargeOrder;
    private Boolean isScheduled;

    private Instant createdAt;
}
