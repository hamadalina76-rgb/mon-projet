package com.speedline.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requête pour mettre à jour une zone
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneUpdateRequest {
    private String name;
    private String description;
    private String city;
    private String boundaryJson; // Format: [[lat,lon],[lat,lon],...]
    private BigDecimal deliveryFee;
    private Integer minDeliveryTime;
    private Integer maxDeliveryTime;
    private Boolean isActive;
    /**
     * Rayon de livraison approximatif pour cette zone (en kilomètres).
     */
    private Integer radiusKm;
}
