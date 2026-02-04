package com.speedline.user.dto;

import com.speedline.user.domain.CustomerPreferences;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour un profil client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateRequest {

    /**
     * Préférences du client
     */
    private CustomerPreferences preferences;

    /**
     * Liste des IDs de partenaires favoris (séparés par virgule)
     */
    private String favoritePartnerIds;

    /**
     * Liste des IDs de produits favoris (séparés par virgule)
     */
    private String favoriteProductIds;
}
