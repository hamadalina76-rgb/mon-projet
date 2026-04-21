package com.speedline.delivery.dispatch.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Shared courier input object for dispatching contract.
 * Contract-Version: 1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCourier {
    private Long id;
    private Long zoneId;

    private CourierType type;
    private Double lat;
    private Double lon;
    private CourierStatus status;

    /** MOTO, MOTOTRICYCLE, BICYCLE, etc. */
    private String vehicleType;

    private Double rating;
    private Integer currentLoad;
    private Integer maxCapacity;

    private LocalTime shiftStart;
    private LocalTime shiftEnd;

    /** DISP-103 — wall-clock instant when a PRE_ASSIGNABLE courier becomes free. */
    private Instant availableFromInstant;
}
