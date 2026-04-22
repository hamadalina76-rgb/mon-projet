package com.speedline.delivery.dispatch.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    /**
     * Données dénormalisées pour création {@code Delivery} (event + order-service)
     */
    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private String partnerName;
    private String pickupAddress;
    private String dropoffAddress;
    private String deliveryInstructions;
    private BigDecimal deliveryFee;

    private Boolean isUrgent;
    private Boolean isLargeOrder;
    private Boolean isScheduled;

    private Instant createdAt;
    private Instant scheduledDeliveryAt;
}
