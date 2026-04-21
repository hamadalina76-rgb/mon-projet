package com.speedline.delivery.matching.cost.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ETAEstimation {
    int courierToPickupMinutes;
    int preparationMinutes;
    int pickupToDropoffMinutes;

    public int totalMinutes() {
        return Math.max(0, courierToPickupMinutes) + Math.max(0, preparationMinutes) + Math.max(0, pickupToDropoffMinutes);
    }
}
