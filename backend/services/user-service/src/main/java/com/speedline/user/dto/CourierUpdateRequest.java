package com.speedline.user.dto;

import com.speedline.user.domain.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour un profil livreur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierUpdateRequest {

    /**
     * Type de véhicule
     */
    private VehicleType vehicleType;

    /**
     * Numéro d'immatriculation
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
     * Zone de livraison préférée
     */
    private String preferredDeliveryZone;

    /**
     * Rayon maximum de livraison (en mètres)
     */
    private Integer maxDeliveryRadius;

    /**
     * IBAN pour les virements
     */
    private String bankIban;

    /**
     * Nom du titulaire du compte bancaire
     */
    private String bankAccountHolder;
}
