package com.speedline.delivery.matching.cost.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class ScoringContext {
    Long deliveryId;
    Long orderId;
    Long courierId;

    BigDecimal courierLatitude;
    BigDecimal courierLongitude;
    BigDecimal pickupLatitude;
    BigDecimal pickupLongitude;
    BigDecimal dropoffLatitude;
    BigDecimal dropoffLongitude;

    BigDecimal currentRouteDropoffLatitude;
    BigDecimal currentRouteDropoffLongitude;

    String courierType;
    String vehicleType;

    boolean courierAvailable;
    boolean courierOnMission;

    BigDecimal rating;
    int activeDeliveries;

    Integer guaranteedDelayMinutes;
    boolean bulkyOrder;
    Integer preparationMinutes;

    LocalDateTime lastRefusalAt;
    LocalDateTime lastCompletedDeliveryAt;
    LocalDateTime lastMerchantDeliveryAt;
}
