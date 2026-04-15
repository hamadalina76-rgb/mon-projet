package com.speedline.location.dto;

import jakarta.validation.constraints.*;
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

    @DecimalMin(value = "0.0",    inclusive = true,  message = "Les frais de livraison ne peuvent pas être négatifs")
    @DecimalMax(value = "999.999", inclusive = true, message = "Les frais de livraison ne peuvent pas dépasser 999.999 TND")
    @Digits(integer = 3, fraction = 3, message = "Format invalide : max 3 entiers et 3 décimales")
    private BigDecimal deliveryFee;

    @DecimalMin(value = "0.0", inclusive = true, message = "Les frais de service ne peuvent pas être négatifs")
    @DecimalMax(value = "999.999", inclusive = true, message = "Les frais de service ne peuvent pas dépasser 999.999 TND")
    @Digits(integer = 3, fraction = 3, message = "Format invalide : max 3 entiers et 3 décimales")
    private BigDecimal serviceFee;

    private Integer minDeliveryTime;
    private Integer maxDeliveryTime;
    private Boolean isActive;

    /**
     * Rayon de livraison approximatif pour cette zone (en kilomètres).
     */
    @Min(value = 0,   message = "Le rayon doit être positif")
    @Max(value = 500, message = "Le rayon ne peut pas dépasser 500 km")
    private Integer radiusKm;
}
