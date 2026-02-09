package com.speedline.user.dto;

import com.speedline.user.domain.CustomerPreferences;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Requête de mise à jour d'un profil client")
public class CustomerUpdateRequest {

    /**
     * Préférences du client
     */
    @Valid
    @Schema(description = "Préférences mises à jour du client")
    private CustomerPreferences preferences;

    /**
     * Liste des IDs de partenaires favoris (séparés par virgule)
     */
    @Pattern(regexp = "^(\\d+(,\\d+)*)?$", message = "Format invalide. Utilisez des IDs séparés par des virgules (ex: 1,2,3)")
    @Size(max = 1000, message = "La liste des partenaires favoris ne peut pas dépasser 1000 caractères")
    @Schema(description = "IDs des partenaires favoris séparés par virgule", example = "1,5,12,45")
    private String favoritePartnerIds;

    /**
     * Liste des IDs de produits favoris (séparés par virgule)
     */
    @Pattern(regexp = "^(\\d+(,\\d+)*)?$", message = "Format invalide. Utilisez des IDs séparés par des virgules (ex: 1,2,3)")
    @Size(max = 1000, message = "La liste des produits favoris ne peut pas dépasser 1000 caractères")
    @Schema(description = "IDs des produits favoris séparés par virgule", example = "10,20,30")
    private String favoriteProductIds;
}
