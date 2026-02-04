package com.speedline.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour la disponibilité d'un livreur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierAvailabilityRequest {

    /**
     * Disponible pour accepter des livraisons
     */
    @NotNull(message = "Le statut de disponibilité est obligatoire")
    private Boolean isAvailable;

    /**
     * En ligne (application active)
     */
    @NotNull(message = "Le statut en ligne est obligatoire")
    private Boolean isOnline;
}
