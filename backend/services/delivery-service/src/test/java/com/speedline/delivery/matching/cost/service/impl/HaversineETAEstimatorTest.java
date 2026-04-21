package com.speedline.delivery.matching.cost.service.impl;

import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineETAEstimatorTest {

    private final HaversineETAEstimator estimator = new HaversineETAEstimator();

    @Test
    void bikeShouldBeSlowerThanMotorcycleForSameRoute() {
        ScoringContext bikeContext = baseContext("BIKE");
        ScoringContext motoContext = baseContext("MOTORCYCLE");

        ETAEstimation bikeEta = estimator.estimate(bikeContext);
        ETAEstimation motoEta = estimator.estimate(motoContext);

        assertTrue(bikeEta.totalMinutes() > motoEta.totalMinutes());
    }

    private ScoringContext baseContext(String vehicleType) {
        return ScoringContext.builder()
                .deliveryId(1L)
                .orderId(10L)
                .courierId(100L)
                .courierLatitude(BigDecimal.valueOf(36.80))
                .courierLongitude(BigDecimal.valueOf(10.18))
                .pickupLatitude(BigDecimal.valueOf(36.81))
                .pickupLongitude(BigDecimal.valueOf(10.19))
                .dropoffLatitude(BigDecimal.valueOf(36.83))
                .dropoffLongitude(BigDecimal.valueOf(10.21))
                .vehicleType(vehicleType)
                .preparationMinutes(8)
                .build();
    }
}
