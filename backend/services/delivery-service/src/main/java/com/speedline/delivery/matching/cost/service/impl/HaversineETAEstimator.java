package com.speedline.delivery.matching.cost.service.impl;

import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.matching.cost.service.ETAEstimator;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class HaversineETAEstimator implements ETAEstimator {

    private final DispatchProperties dispatchProperties;

    public HaversineETAEstimator() {
        this(new DispatchProperties());
    }

    @Autowired
    public HaversineETAEstimator(DispatchProperties dispatchProperties) {
        this.dispatchProperties = dispatchProperties;
    }

    @Override
    public ETAEstimation estimate(ScoringContext context) {
        double speedKmh = averageSpeedKmh(context.getVehicleType());

        int courierToPickup = estimateMinutes(
                context.getCourierLatitude(),
                context.getCourierLongitude(),
                context.getPickupLatitude(),
                context.getPickupLongitude(),
                speedKmh
        );

        int pickupToDropoff = estimateMinutes(
                context.getPickupLatitude(),
                context.getPickupLongitude(),
                context.getDropoffLatitude(),
                context.getDropoffLongitude(),
                speedKmh
        );

        return ETAEstimation.builder()
                .courierToPickupMinutes(courierToPickup)
                .preparationMinutes(context.getPreparationMinutes() != null ? context.getPreparationMinutes() : 10)
                .pickupToDropoffMinutes(pickupToDropoff)
                .build();
    }

    private int estimateMinutes(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2, double speedKmh) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null || speedKmh <= 0) {
            return 0;
        }

        double distanceKm = haversineKm(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
        double minutes = (distanceKm / speedKmh) * 60.0;
        return (int) Math.ceil(Math.max(0.0, minutes));
    }

    private double averageSpeedKmh(String vehicleType) {
        if (vehicleType == null) {
            return dispatchProperties.getMatching().getEta().getDefaultSpeedKmh();
        }

        return switch (vehicleType.trim().toUpperCase()) {
            case "MOTORCYCLE" -> dispatchProperties.getMatching().getEta().getMotorcycleSpeedKmh();
            case "BIKE" -> dispatchProperties.getMatching().getEta().getBikeSpeedKmh();
            case "MOTOR_TRICYCLE", "TRICYCLE" -> dispatchProperties.getMatching().getEta().getMotorTricycleSpeedKmh();
            case "CAR" -> dispatchProperties.getMatching().getEta().getCarSpeedKmh();
            default -> dispatchProperties.getMatching().getEta().getDefaultSpeedKmh();
        };
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
