package com.speedline.user.dto;

import com.speedline.user.domain.VehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer un nouveau profil livreur
 * Appelé après l'inscription dans auth-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierCreateRequest {

    /**
     * ID de l'utilisateur dans auth-service (obligatoire)
     */
    @NotNull(message = "L'ID utilisateur est obligatoire")
    private Long userId;

    /**
     * Type de véhicule (optionnel lors de la création initiale)
     */
    private VehicleType vehicleType;

    /**
     * Numéro d'immatriculation (optionnel pour vélo)
     */
    private String vehicleNumber;

    /**
     * Marque et modèle du véhicule
     */
    private String vehicleModel;

    /**
     * Couleur du véhicule
     */
    private String vehicleColor;

    /**
     * Numéro du permis de conduire
     */
    private String drivingLicenseNumber;

    /**
     * Zone de livraison préférée
     */
    private String preferredDeliveryZone;

    /**
     * Rayon maximum de livraison (en mètres)
     */
    private Integer maxDeliveryRadius;
}
