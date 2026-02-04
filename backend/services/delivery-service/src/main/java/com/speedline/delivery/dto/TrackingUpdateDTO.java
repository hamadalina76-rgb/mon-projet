package com.speedline.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour mise à jour de la position GPS du livreur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingUpdateDTO {

    /**
     * ID de la livraison
     */
    @NotNull(message = "L'ID de la livraison est obligatoire")
    private Long deliveryId;

    /**
     * Latitude GPS
     */
    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private BigDecimal latitude;

    /**
     * Longitude GPS
     */
    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private BigDecimal longitude;

    /**
     * Précision de la position (en mètres)
     */
    private BigDecimal accuracy;

    /**
     * Altitude (en mètres)
     */
    private BigDecimal altitude;

    /**
     * Vitesse (en km/h)
     */
    private BigDecimal speed;

    /**
     * Direction (bearing) en degrés (0-360)
     */
    private BigDecimal bearing;

    /**
     * Niveau de batterie du téléphone (0-100)
     */
    private Integer batteryLevel;
}
