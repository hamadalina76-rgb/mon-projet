package com.speedline.location.dto;

import com.speedline.location.domain.Zone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requête pour créer une nouvelle zone
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneCreateRequest {

    @NotBlank(message = "Le nom de la zone est requis")
    private String name;

    private String description;

    @NotBlank(message = "La ville est requise")
    private String city;

    @NotNull(message = "Le type de zone est requis")
    private Zone.ZoneType type;

    @NotBlank(message = "Le polygone de la zone est requis")
    private String boundaryJson; // Format: [[lat,lon],[lat,lon],...]

    @NotNull(message = "Les frais de livraison sont obligatoires")
    @DecimalMin(value = "0.0",  inclusive = true,  message = "Les frais de livraison ne peuvent pas être négatifs")
    @DecimalMax(value = "999.999", inclusive = true, message = "Les frais de livraison ne peuvent pas dépasser 999.999 TND")
    @Digits(integer = 3, fraction = 3, message = "Format invalide : max 3 entiers et 3 décimales")
    private BigDecimal deliveryFee;

    @DecimalMin(value = "0.0", inclusive = true, message = "Les frais de service ne peuvent pas être négatifs")
    @DecimalMax(value = "999.999", inclusive = true, message = "Les frais de service ne peuvent pas dépasser 999.999 TND")
    @Digits(integer = 3, fraction = 3, message = "Format invalide : max 3 entiers et 3 décimales")
    private BigDecimal serviceFee;

    private Integer minDeliveryTime;

    private Integer maxDeliveryTime;

    /**
     * Rayon de livraison approximatif pour cette zone (en kilomètres).
     */
    @Min(value = 0,   message = "Le rayon doit être positif")
    @Max(value = 500, message = "Le rayon ne peut pas dépasser 500 km")
    private Integer radiusKm;

    @Builder.Default
    private Boolean isActive = true;
}
