package com.speedline.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Réponse représentant une option unitaire (OptionValue) à l'intérieur d'un groupe.
 * TC-17 : priceModifier=0 retourné sans erreur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {

    private Long id;

    /** ID du groupe (ProductOption) auquel cette option appartient. */
    private Long groupId;

    private String name;

    /**
     * Supplément de prix (0 = gratuit, positif = surcharge).
     * TC-17 : valeur 0 acceptée et retournée.
     */
    private BigDecimal priceModifier;

    /** Pré-sélectionnée par défaut. */
    private Boolean isDefault;

    private Boolean isAvailable;

    /** Position dans le groupe (ordre croissant). */
    private Integer position;
}
